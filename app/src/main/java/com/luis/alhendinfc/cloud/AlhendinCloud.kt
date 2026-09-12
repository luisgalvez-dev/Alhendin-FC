package com.luis.alhendinfc.cloud

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.luis.alhendinfc.cloud.auth.AuthRepository
import com.luis.alhendinfc.cloud.auth.AuthSession
import com.luis.alhendinfc.cloud.auth.DataStoreMembershipCache
import com.luis.alhendinfc.cloud.auth.FirebaseAuthBackend
import com.luis.alhendinfc.cloud.auth.UnavailableAuthBackend
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.dev.DevSeedData
import com.luis.alhendinfc.data.preferences.HomePreferencesRepository
import com.luis.alhendinfc.data.sync.SyncEngine
import com.luis.alhendinfc.data.sync.SyncEntityType
import com.luis.alhendinfc.data.sync.SyncRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AlhendinCloud private constructor(
    context: Context,
    val store: CloudStore,
    val auth: AuthRepository,
    val engine: SyncEngine,
    val homePrefs: HomePreferencesRepository
) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _session = MutableStateFlow<AuthSession>(AuthSession.Checking)
    val session: StateFlow<AuthSession> = _session.asStateFlow()
    private val handles = mutableListOf<CloudListenHandle>()
    @Volatile
    var online: Boolean = true
        private set

    fun start() {
        observeConnectivity()
        observeLifecycle()
        scope.launch {
            homePrefs.migrateToDeviceLayoutIfNeeded()
            refreshSession()
        }
        scope.launch {
            AlhendinDatabase.getInstance(app).syncOutboxDao().observeCount()
                .distinctUntilChanged()
                .debounce(500)
                .collect { count ->
                    if (count > 0 && _session.value is AuthSession.Ready) {
                        engine.sync()
                    }
                }
        }
    }

    suspend fun refreshSession() {
        val keepVisible = _session.value is AuthSession.Ready
        if (!keepVisible) {
            _session.value = AuthSession.Checking
        }
        val resolved = try {
            auth.resolveCurrent()
        } catch (_: Exception) {
            if (keepVisible) return else AuthSession.LoggedOut
        }
        applySession(resolved)
        if (resolved is AuthSession.Ready) {
            val confirmed = try {
                auth.confirmMembership()
            } catch (_: Exception) {
                return
            }
            if (confirmed is AuthSession.NonMember) {
                applySession(confirmed)
            }
        }
    }

    suspend fun signIn(email: String, password: String): AuthSession {
        val resolved = auth.signIn(email, password)
        applySession(resolved)
        return resolved
    }

    suspend fun signOut() {
        stopListeners()
        auth.signOut()
        _session.value = AuthSession.LoggedOut
    }

    private suspend fun applySession(resolved: AuthSession) {
        _session.value = resolved
        if (resolved is AuthSession.Ready) {
            startListeners()
            engine.sync()
        } else {
            stopListeners()
        }
    }

    private fun startListeners() {
        stopListeners()
        SyncEntityType.DOWNLOAD_ORDER.forEach { type ->
            handles += store.listenCollection(type) { docs ->
                scope.launch {
                    engine.applyRemoteCollection(type, docs)
                }
            }
        }
    }

    private fun stopListeners() {
        handles.forEach { it.close() }
        handles.clear()
    }

    private fun observeConnectivity() {
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        online = cm.isCurrentlyOnline()
        try {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    online = true
                    scope.launch {
                        refreshSession()
                        if (_session.value is AuthSession.Ready) engine.sync()
                    }
                }

                override fun onLost(network: Network) {
                    online = cm.isCurrentlyOnline()
                }
            })
        } catch (_: Exception) {
        }
    }

    private fun observeLifecycle() {
        val owner = ProcessLifecycleOwner.get()
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                scope.launch {
                    if (_session.value is AuthSession.Ready) {
                        delay(300)
                        engine.sync()
                    }
                }
            }
        })
    }

    companion object {
        @Volatile
        private var INSTANCE: AlhendinCloud? = null

        fun getInstance(context: Context): AlhendinCloud {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: create(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun createForTests(
            context: Context,
            store: CloudStore,
            auth: AuthRepository,
            engine: SyncEngine,
            homePrefs: HomePreferencesRepository
        ): AlhendinCloud = AlhendinCloud(context, store, auth, engine, homePrefs)

        private fun create(context: Context): AlhendinCloud {
            val db = AlhendinDatabase.getInstance(context)
            val configured = FirebaseAvailability.isConfigured(context)
            val firebaseReady = configured && runCatching { FirebaseApp.getInstance() }.isSuccess
            val store: CloudStore
            val authRepo: AuthRepository
            if (firebaseReady) {
                store = FirestoreCloudStore(FirebaseFirestore.getInstance(), CloudConfig.workspaceId)
                authRepo = AuthRepository(
                    FirebaseAuthBackend(FirebaseAuth.getInstance()),
                    store,
                    CloudConfig.workspaceId,
                    DataStoreMembershipCache(context)
                )
            } else {
                store = UnavailableCloudStore()
                authRepo = AuthRepository(
                    UnavailableAuthBackend(),
                    store,
                    CloudConfig.workspaceId,
                    DataStoreMembershipCache(context)
                )
            }
            val engine = SyncEngine(
                db = db,
                store = store,
                registry = SyncRegistry(db),
                isOnline = {
                    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    cm?.isCurrentlyOnline() ?: true
                },
                onWonBootstrap = { DevSeedData.runIfNeeded(context) }
            )
            return AlhendinCloud(
                context,
                store,
                authRepo,
                engine,
                HomePreferencesRepository.getInstance(context)
            )
        }
    }
}

private fun ConnectivityManager.isCurrentlyOnline(): Boolean {
    val network = activeNetwork ?: return false
    val caps = getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
