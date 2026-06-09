package ink.duo3.tuned.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import ink.duo3.tuned.domain.model.InteractionSettings
import ink.duo3.tuned.domain.repository.InteractionSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class DataStoreInteractionSettingsRepository(
    context: Context,
) : InteractionSettingsRepository {
    private val dataStore = context.applicationContext.tunedSettingsDataStore

    override val interactionSettings: Flow<InteractionSettings> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }.map { preferences ->
                InteractionSettings(
                    hapticFeedbackEnabled = preferences[HAPTIC_FEEDBACK_ENABLED] ?: true,
                    usePreciseTime = preferences[USE_PRECISE_TIME] ?: false,
                )
            }

    override suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }

    override suspend fun setUsePreciseTime(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_PRECISE_TIME] = enabled
        }
    }

    private companion object {
        val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val USE_PRECISE_TIME = booleanPreferencesKey("use_precise_time")
    }
}
