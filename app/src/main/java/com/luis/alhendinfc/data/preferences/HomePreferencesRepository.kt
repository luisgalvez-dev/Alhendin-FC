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
 * Preferencias personales/locales del Inicio (orden y visibilidad de módulos).
 * No forma parte del dataset deportivo compartido. El Inicio por usuario llega tras Auth.
 */
class HomePreferencesRepository(private val dataStore: DataStore<Preferences>) {

    val layoutConfig: Flow<HomeLayoutConfig> = dataStore.data.map { prefs ->
        decode(prefs[KEY_LAYOUT])
    }

    suspend fun setEnabled(module: HomeModule, enabled: Boolean) {
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
        val prefs = dataStore.data.map { it[KEY_LAYOUT] }.first()
        return prefs ?: encode(HomeLayoutConfig.defaults())
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
        dataStore.edit { it[KEY_LAYOUT] = encode(HomeLayoutConfig.defaults()) }
    }

    private suspend fun update(transform: (List<HomeModulePreference>) -> List<HomeModulePreference>) {
        dataStore.edit { prefs ->
            val current = decode(prefs[KEY_LAYOUT]).modules
            prefs[KEY_LAYOUT] = encode(HomeLayoutConfig(transform(current)))
        }
    }

    companion object {
        private val KEY_LAYOUT = stringPreferencesKey("home_layout")

        /** Formato: `team:1,matches:1,...` (1=activo, 0=oculto; el orden es el de la lista). */
        fun encode(config: HomeLayoutConfig): String =
            config.modules.joinToString(",") { "${it.module.id}:${if (it.enabled) 1 else 0}" }

        fun decode(raw: String?): HomeLayoutConfig {
            if (raw.isNullOrBlank()) return HomeLayoutConfig.defaults()
            val parsed = raw.split(',')
                .mapNotNull { token ->
                    val parts = token.split(':')
                    if (parts.size != 2) return@mapNotNull null
                    val module = HomeModule.fromId(parts[0].trim()) ?: return@mapNotNull null
                    val enabled = parts[1].trim() != "0"
                    HomeModulePreference(module, enabled)
                }
            if (parsed.isEmpty()) return HomeLayoutConfig.defaults()

            val seen = parsed.map { it.module }.toSet()
            val missing = HomeModule.DEFAULT_ORDER
                .filter { it !in seen }
                .map { HomeModulePreference(it, enabled = true) }
            return HomeLayoutConfig(parsed + missing)
        }

        fun getInstance(context: Context): HomePreferencesRepository =
            HomePreferencesRepository(context.applicationContext.homeDataStore)
    }
}
