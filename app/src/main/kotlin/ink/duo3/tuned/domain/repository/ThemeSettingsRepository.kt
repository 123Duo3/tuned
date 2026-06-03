package ink.duo3.tuned.domain.repository

import ink.duo3.tuned.domain.model.ThemeSettings
import kotlinx.coroutines.flow.Flow

interface ThemeSettingsRepository {
    val themeSettings: Flow<ThemeSettings>

    suspend fun setFollowSystemAppearance(followSystemAppearance: Boolean)

    suspend fun setUseDarkMode(useDarkMode: Boolean)

    suspend fun setUseMonet(useMonet: Boolean)

    suspend fun setMonetSeed(monetSeed: Int)
}
