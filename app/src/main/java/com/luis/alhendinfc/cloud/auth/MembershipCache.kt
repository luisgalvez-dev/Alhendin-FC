package com.luis.alhendinfc.cloud.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/**
 * Último membership validado en este dispositivo.
 * Permite arrancar offline con Room si FirebaseAuth conserva la sesión
 * y ese uid ya fue aceptado para el workspace.
 */
interface MembershipCache {
    suspend fun isAuthorized(uid: String, workspaceId: String): Boolean
    suspend fun remember(uid: String, workspaceId: String)
    suspend fun forget(uid: String)
}

class MemoryMembershipCache : MembershipCache {
    @Volatile
    private var uid: String? = null
    @Volatile
    private var workspaceId: String? = null

    override suspend fun isAuthorized(uid: String, workspaceId: String): Boolean {
        return this.uid == uid && this.workspaceId == workspaceId
    }

    override suspend fun remember(uid: String, workspaceId: String) {
        this.uid = uid
        this.workspaceId = workspaceId
    }

    override suspend fun forget(uid: String) {
        if (this.uid == uid) {
            this.uid = null
            this.workspaceId = null
        }
    }

    fun seed(uid: String, workspaceId: String) {
        this.uid = uid
        this.workspaceId = workspaceId
    }
}

private val Context.membershipDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "membership_cache"
)

class DataStoreMembershipCache(context: Context) : MembershipCache {
    private val dataStore = context.applicationContext.membershipDataStore

    override suspend fun isAuthorized(uid: String, workspaceId: String): Boolean {
        val prefs = dataStore.data.first()
        return prefs[KEY_UID] == uid && prefs[KEY_WORKSPACE] == workspaceId
    }

    override suspend fun remember(uid: String, workspaceId: String) {
        dataStore.edit {
            it[KEY_UID] = uid
            it[KEY_WORKSPACE] = workspaceId
        }
    }

    override suspend fun forget(uid: String) {
        dataStore.edit { prefs ->
            if (prefs[KEY_UID] == uid) {
                prefs.remove(KEY_UID)
                prefs.remove(KEY_WORKSPACE)
            }
        }
    }

    companion object {
        private val KEY_UID = stringPreferencesKey("membership_uid")
        private val KEY_WORKSPACE = stringPreferencesKey("membership_workspace")
    }
}
