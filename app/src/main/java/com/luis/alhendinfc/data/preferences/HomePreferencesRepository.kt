package com.luis.alhendinfc.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.luis.alhendinfc.domain.model.HomeLayoutConfig
import com.luis.alhendinfc.domain.model.HomeModule
import com.luis.alhendinfc.domain.model.HomeModulePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.homeDataStore: DataStore<Preferences> by preferencesDataStore(name = "home_prefs")

/**
 * Preferencias locales del Inicio (orden y visibilidad de módulos).
 * No forma parte del dataset deportivo compartido. Pertenecen al dispositivo,
 * no al usuario de Auth. No se sincronizan por Firebase.
 */
class HomePreferencesRepository(private val dataStore: DataStore<Preferences>) {

    val layoutConfig: Flow<HomeLayoutConfig> = dataStore.data.map { prefs ->
        decode(readEncoded(prefs))
    }

    suspend fun migrateToDeviceLayoutIfNeeded() {
        dataStore.edit { prefs ->
            if (!prefs[KEY_LAYOUT].isNullOrBlank()) return@edit
            val legacy = firstLegacyLayout(prefs) ?: return@edit
            prefs[KEY_LAYOUT] = legacy
        }
    }

    suspend fun setEnabled(module: HomeModule, enabled: Boolean) {
        if (module == HomeModule.SETTINGS) return
        update { current ->
            current.map {
                if (it.module == module) it.copy(enabled = enabled) else it
            }
        }
    }

    suspend fun restoreEncodedLayout(raw: String?) {
        dataStore.edit {
            it[KEY_LAYOUT] = encode(decode(raw))
        }
    }

    suspend fun currentEncodedLayout(): String {
        val prefs = dataStore.data.first()
        return readEncoded(prefs) ?: encode(HomeLayoutConfig.defaults())
    }

    suspend fun moveUp(module: HomeModule) {
        update { current ->
            val index = current.indexOfFirst { it.module == module }
            if (index <= 0) current
            else current.toMutableList().also {
                val item = it.removeAt(index)
                it.add(index - 1, item)
            }
        }
    }

    suspend fun moveDown(module: HomeModule) {
        update { current ->
            val index = current.indexOfFirst { it.module == module }
            if (index < 0 || index >= current.lastIndex) current
            else current.toMutableList().also {
                val item = it.removeAt(index)
                it.add(index + 1, item)
            }
        }
    }

    suspend fun resetDefaults() {
        persist(encode(HomeLayoutConfig.defaults()))
    }

    private suspend fun update(transform: (List<HomeModulePreference>) -> List<HomeModulePreference>) {
        dataStore.edit { prefs ->
            val current = decode(readEncoded(prefs)).modules
            prefs[KEY_LAYOUT] = encode(HomeLayoutConfig(transform(current)))
        }
    }

    private suspend fun persist(encoded: String) {
        dataStore.edit { it[KEY_LAYOUT] = encoded }
    }

    companion object {
        private val KEY_LAYOUT = stringPreferencesKey("home_layout")

        fun keyFor(uid: String?): Preferences.Key<String> =
            if (uid.isNullOrBlank()) {
                stringPreferencesKey("home_layout_anon")
            } else {
                stringPreferencesKey("home_layout_$uid")
            }

        /** Formato: `team:1,matches:1,...` (1=activo, 0=oculto; el orden es el de la lista). */
        fun encode(config: HomeLayoutConfig): String =
            config.modules.joinToString(",") { "${it.module.id}:${if (it.enabled) 1 else 0}" }

        fun decode(raw: String?): HomeLayoutConfig {
            if (raw.isNullOrBlank()) return forceSettingsVisible(HomeLayoutConfig.defaults())
            val parsed = raw.split(',')
                .mapNotNull { token ->
                    val parts = token.split(':')
                    if (parts.size != 2) return@mapNotNull null
                    val module = HomeModule.fromId(parts[0].trim()) ?: return@mapNotNull null
                    val enabled = parts[1].trim() != "0"
                    HomeModulePreference(module, enabled)
                }
            if (parsed.isEmpty()) return forceSettingsVisible(HomeLayoutConfig.defaults())

            val result = parsed.toMutableList()
            val seen = result.map { it.module }.toMutableSet()
            HomeModule.DEFAULT_ORDER.forEach { module ->
                if (module in seen) return@forEach
                val predecessors = HomeModule.DEFAULT_ORDER.take(
                    HomeModule.DEFAULT_ORDER.indexOf(module)
                )
                val insertAfter = result.indexOfLast { it.module in predecessors }
                val pref = HomeModulePreference(module, enabled = true)
                if (insertAfter >= 0) result.add(insertAfter + 1, pref) else result.add(0, pref)
                seen += module
            }
            return forceSettingsVisible(HomeLayoutConfig(result))
        }

        private fun forceSettingsVisible(config: HomeLayoutConfig): HomeLayoutConfig {
            return HomeLayoutConfig(
                config.modules.map {
                    if (it.module == HomeModule.SETTINGS) it.copy(enabled = true) else it
                }
            )
        }

        private fun readEncoded(prefs: Preferences): String? {
            val current = prefs[KEY_LAYOUT]
            if (!current.isNullOrBlank()) return current
            return firstLegacyLayout(prefs)
        }

        private fun firstLegacyLayout(prefs: Preferences): String? {
            val anon = prefs[stringPreferencesKey("home_layout_anon")]
            if (!anon.isNullOrBlank()) return anon
            prefs.asMap().forEach { (key, value) ->
                if (key.name.startsWith("home_layout_") && value is String && value.isNotBlank()) {
                    return value
                }
            }
            return null
        }

        fun getInstance(context: Context): HomePreferencesRepository =
            HomePreferencesRepository(context.applicationContext.homeDataStore)
    }
}
