package ink.duo3.tuned.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.duo3.tuned.domain.repository.ThemeSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val themeSettingsRepository: ThemeSettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        themeSettingsRepository.themeSettings
            .map { SettingsUiState(themeSettings = it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = SettingsUiState(),
            )

    fun setFollowSystemAppearance(followSystemAppearance: Boolean) {
        viewModelScope.launch {
            themeSettingsRepository.setFollowSystemAppearance(followSystemAppearance)
        }
    }

    fun setUseDarkMode(useDarkMode: Boolean) {
        viewModelScope.launch {
            themeSettingsRepository.setUseDarkMode(useDarkMode)
        }
    }

    fun setUseMonet(useMonet: Boolean) {
        viewModelScope.launch {
            themeSettingsRepository.setUseMonet(useMonet)
        }
    }

    fun setMonetSeed(monetSeed: Int) {
        viewModelScope.launch {
            themeSettingsRepository.setMonetSeed(monetSeed)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
