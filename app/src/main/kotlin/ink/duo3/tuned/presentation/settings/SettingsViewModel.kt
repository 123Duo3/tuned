package ink.duo3.tuned.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.repository.InteractionSettingsRepository
import ink.duo3.tuned.domain.repository.OpmlRepository
import ink.duo3.tuned.domain.repository.ThemeSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the settings screen: the persisted [ThemeSettings] flow combined with
 * view-only transient OPML state (busy + a one-shot result event). Per the project
 * rule, the screen collects exactly one [uiState].
 */
class SettingsViewModel(
    private val themeSettingsRepository: ThemeSettingsRepository,
    private val interactionSettingsRepository: InteractionSettingsRepository,
    private val opmlRepository: OpmlRepository,
) : ViewModel() {
    private val transient = MutableStateFlow(Transient())

    val uiState: StateFlow<SettingsUiState> =
        combine(
            themeSettingsRepository.themeSettings,
            interactionSettingsRepository.interactionSettings,
            transient,
        ) { themeSettings, interactionSettings, t ->
            SettingsUiState(
                themeSettings = themeSettings,
                interactionSettings = interactionSettings,
                isOpmlBusy = t.isOpmlBusy,
                opmlEvent = t.opmlEvent,
            )
        }.stateIn(
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

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            interactionSettingsRepository.setHapticFeedbackEnabled(enabled)
        }
    }

    fun importOpml(content: String) {
        if (transient.value.isOpmlBusy) return
        transient.update { it.copy(isOpmlBusy = true) }
        viewModelScope.launch {
            val event =
                when (val outcome = opmlRepository.import(content)) {
                    is Outcome.Success ->
                        OpmlEvent.Imported(
                            imported = outcome.value.imported,
                            failed = outcome.value.failed,
                        )

                    is Outcome.Failure -> OpmlEvent.ImportFailed
                }
            transient.update { it.copy(isOpmlBusy = false, opmlEvent = event) }
        }
    }

    fun exportOpml() {
        if (transient.value.isOpmlBusy) return
        transient.update { it.copy(isOpmlBusy = true) }
        viewModelScope.launch {
            val event =
                when (val outcome = opmlRepository.export()) {
                    is Outcome.Success -> OpmlEvent.ExportReady(outcome.value)
                    is Outcome.Failure -> OpmlEvent.ExportFailed
                }
            transient.update { it.copy(isOpmlBusy = false, opmlEvent = event) }
        }
    }

    fun consumeOpmlEvent() = transient.update { it.copy(opmlEvent = null) }

    private data class Transient(
        val isOpmlBusy: Boolean = false,
        val opmlEvent: OpmlEvent? = null,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
