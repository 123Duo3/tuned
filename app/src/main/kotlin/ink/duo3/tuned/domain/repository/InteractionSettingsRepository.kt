package ink.duo3.tuned.domain.repository

import ink.duo3.tuned.domain.model.InteractionSettings
import kotlinx.coroutines.flow.Flow

interface InteractionSettingsRepository {
    val interactionSettings: Flow<InteractionSettings>

    suspend fun setHapticFeedbackEnabled(enabled: Boolean)

    suspend fun setUsePreciseTime(enabled: Boolean)
}
