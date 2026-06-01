package ink.duo3.tuned.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the library screen. The subscription list is the repository's Room flow,
 * combined with view-only transient state (which podcasts are mid-refresh, the last
 * refresh error). Per the project rule, the screen collects exactly one [uiState].
 */
class LibraryViewModel(
    private val repository: PodcastRepository,
) : ViewModel() {
    private val transient = MutableStateFlow(Transient())

    val uiState: StateFlow<LibraryUiState> =
        combine(repository.observeSubscriptions(), transient) { podcasts, t ->
            LibraryUiState(
                isLoading = false,
                podcasts = podcasts,
                refreshingIds = t.refreshingIds,
                refreshError = t.refreshError,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LibraryUiState())

    fun refresh(podcastId: String) {
        if (podcastId in transient.value.refreshingIds) return
        transient.update { it.copy(refreshingIds = it.refreshingIds + podcastId) }
        viewModelScope.launch {
            val error = (repository.refresh(podcastId) as? Outcome.Failure)?.error
            transient.update {
                it.copy(refreshingIds = it.refreshingIds - podcastId, refreshError = error ?: it.refreshError)
            }
        }
    }

    fun consumeError() = transient.update { it.copy(refreshError = null) }

    private data class Transient(
        val refreshingIds: Set<String> = emptySet(),
        val refreshError: AppError? = null,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
