package ink.duo3.tuned.presentation.podcast

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
 * Drives the podcast detail screen for one [podcastId]. The podcast metadata and its
 * episode list are the repository's Room flows, combined with view-only transient state
 * (an in-flight refresh, the last refresh error). The screen collects exactly one
 * [uiState]. [podcastId] is passed per-instance via Koin `parametersOf`.
 */
class PodcastDetailViewModel(
    private val podcastId: String,
    private val repository: PodcastRepository,
) : ViewModel() {
    private val transient = MutableStateFlow(Transient())

    val uiState: StateFlow<PodcastDetailUiState> =
        combine(
            repository.observePodcast(podcastId),
            repository.observeEpisodes(podcastId),
            transient,
        ) { podcast, episodes, t ->
            PodcastDetailUiState(
                isLoading = false,
                podcast = podcast,
                episodes = episodes,
                isRefreshing = t.isRefreshing,
                refreshError = t.refreshError,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PodcastDetailUiState())

    fun refresh() {
        if (transient.value.isRefreshing) return
        transient.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val error = (repository.refresh(podcastId) as? Outcome.Failure)?.error
            transient.update { it.copy(isRefreshing = false, refreshError = error ?: it.refreshError) }
        }
    }

    fun consumeError() = transient.update { it.copy(refreshError = null) }

    private data class Transient(
        val isRefreshing: Boolean = false,
        val refreshError: AppError? = null,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
