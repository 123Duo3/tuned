package ink.duo3.tuned.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import ink.duo3.tuned.domain.model.ThemeSettings
import ink.duo3.tuned.domain.repository.ThemeSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class DataStoreThemeSettingsRepository(
    context: Context,
) : ThemeSettingsRepository {
    private val dataStore = context.applicationContext.tunedSettingsDataStore

    override val themeSettings: Flow<ThemeSettings> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }.map { preferences ->
                ThemeSettings(
                    followSystemAppearance = preferences[FOLLOW_SYSTEM_APPEARANCE] ?: true,
                    useDarkMode = preferences[USE_DARK_MODE] ?: false,
                    useMonet = preferences[USE_MONET] ?: false,
                    monetSeed = preferences[MONET_SEED] ?: ThemeSettings.MONET_SEED_SYSTEM,
                )
            }

    override suspend fun setFollowSystemAppearance(followSystemAppearance: Boolean) {
        dataStore.edit { preferences ->
            preferences[FOLLOW_SYSTEM_APPEARANCE] = followSystemAppearance
        }
    }

    override suspend fun setUseDarkMode(useDarkMode: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_DARK_MODE] = useDarkMode
        }
    }

    override suspend fun setUseMonet(useMonet: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_MONET] = useMonet
        }
    }

    override suspend fun setMonetSeed(monetSeed: Int) {
        dataStore.edit { preferences ->
            preferences[MONET_SEED] = monetSeed
        }
    }

    private companion object {
        val FOLLOW_SYSTEM_APPEARANCE = booleanPreferencesKey("follow_system_appearance")
        val USE_DARK_MODE = booleanPreferencesKey("use_dark_mode")
        val USE_MONET = booleanPreferencesKey("use_monet")
        val MONET_SEED = intPreferencesKey("monet_seed")
    }
}
