package com.luis.alhendinfc.cloud.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.luis.alhendinfc.cloud.CloudStore
import com.luis.alhendinfc.cloud.FirebaseAvailability
import com.luis.alhendinfc.data.sync.UnavailableCloudException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

interface AuthBackend {
    fun currentUser(): AuthUser?
    fun authState(): Flow<AuthUser?>
    suspend fun signIn(email: String, password: String): AuthUser
    suspend fun signOut()
}

class FirebaseAuthBackend(private val auth: FirebaseAuth) : AuthBackend {
    override fun currentUser(): AuthUser? = auth.currentUser?.toAuthUser()

    override fun authState(): Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toAuthUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): AuthUser {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: throw IllegalStateException("empty")
        return user.toAuthUser()
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email.orEmpty(),
        displayName = displayName?.ifBlank { null } ?: email?.substringBefore('@').orEmpty()
    )
}

class FakeAuthBackend(
    initial: AuthUser? = null
) : AuthBackend {
    @Volatile
    var user: AuthUser? = initial
        private set
    var failWith: Throwable? = null

    override fun currentUser(): AuthUser? = user

    override fun authState(): Flow<AuthUser?> = flowOf(user)

    override suspend fun signIn(email: String, password: String): AuthUser {
        failWith?.let { throw it }
        val signed = AuthUser(
            uid = "uid-${email.hashCode()}",
            email = email,
            displayName = email.substringBefore('@')
        )
        user = signed
        return signed
    }

    override suspend fun signOut() {
        user = null
    }

    fun setUser(value: AuthUser?) {
        user = value
    }
}

class UnavailableAuthBackend(
    private val message: String = FirebaseAvailability.NOT_CONFIGURED
) : AuthBackend {
    override fun currentUser(): AuthUser? = null
    override fun authState(): Flow<AuthUser?> = flowOf(null)
    override suspend fun signIn(email: String, password: String): AuthUser {
        throw UnavailableCloudException(message)
    }
    override suspend fun signOut() = Unit
}

class AuthRepository(
    private val backend: AuthBackend,
    private val store: CloudStore,
    private val workspaceId: String,
    private val membership: MembershipCache = MemoryMembershipCache(),
    private val membershipTimeoutMs: Long = 5_000L
) {
    fun currentUser(): AuthUser? = backend.currentUser()
    fun authState(): Flow<AuthUser?> = backend.authState()

    suspend fun signIn(email: String, password: String): AuthSession {
        val user = try {
            backend.signIn(email, password)
        } catch (e: UnavailableCloudException) {
            return AuthSession.Unavailable(e.message ?: FirebaseAvailability.NOT_CONFIGURED)
        } catch (e: Exception) {
            throw e
        }
        return resolve(user)
    }

    suspend fun signOut() {
        val uid = backend.currentUser()?.uid
        backend.signOut()
        if (uid != null) membership.forget(uid)
    }

    suspend fun resolveCurrent(): AuthSession {
        val user = backend.currentUser() ?: return AuthSession.LoggedOut
        if (membership.isAuthorized(user.uid, workspaceId)) {
            return AuthSession.Ready(user, workspaceId)
        }
        return resolveFromNetwork(user)
    }

    suspend fun confirmMembership(): AuthSession {
        val user = backend.currentUser() ?: return AuthSession.LoggedOut
        return resolveFromNetwork(user)
    }

    private suspend fun resolve(user: AuthUser): AuthSession = resolveFromNetwork(user)

    private suspend fun resolveFromNetwork(user: AuthUser): AuthSession {
        val cached = membership.isAuthorized(user.uid, workspaceId)
        val queried = queryMembership(user.uid)
        return when (queried) {
            MembershipQuery.Member -> {
                membership.remember(user.uid, workspaceId)
                publishProfile(user)
                AuthSession.Ready(user, workspaceId)
            }
            MembershipQuery.NotMember -> {
                membership.forget(user.uid)
                AuthSession.NonMember(user)
            }
            MembershipQuery.Unavailable -> AuthSession.Unavailable(FirebaseAvailability.NOT_CONFIGURED)
            MembershipQuery.Unknown -> {
                if (cached) {
                    AuthSession.Ready(user, workspaceId)
                } else {
                    AuthSession.Unavailable(
                        "Sin conexión. No se pudo verificar el acceso a este espacio."
                    )
                }
            }
        }
    }

    private suspend fun queryMembership(uid: String): MembershipQuery {
        return try {
            withTimeout(membershipTimeoutMs) {
                if (store.isMember(uid)) MembershipQuery.Member else MembershipQuery.NotMember
            }
        } catch (_: TimeoutCancellationException) {
            MembershipQuery.Unknown
        } catch (_: UnavailableCloudException) {
            MembershipQuery.Unavailable
        } catch (_: Exception) {
            MembershipQuery.Unknown
        }
    }

    private suspend fun publishProfile(user: AuthUser) {
        try {
            withTimeout(membershipTimeoutMs) {
                store.putUser(
                    user.uid,
                    mapOf(
                        "displayName" to user.displayName,
                        "email" to user.email
                    )
                )
            }
        } catch (_: Exception) {
        }
    }
}

private enum class MembershipQuery {
    Member,
    NotMember,
    Unknown,
    Unavailable
}
