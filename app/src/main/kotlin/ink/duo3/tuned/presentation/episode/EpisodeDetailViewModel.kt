package ink.duo3.tuned.presentation.episode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.duo3.tuned.domain.repository.PodcastRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Drives the episode detail screen for one [episodeId]. The episode is a Room flow;
 * once it resolves, the parent podcast is looked up so the screen can show its title
 * and fall back to its artwork. The screen collects exactly one [uiState].
 * [episodeId] is passed per-instance via Koin `parametersOf`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EpisodeDetailViewModel(
    private val episodeId: String,
    private val repository: PodcastRepository,
) : ViewModel() {
    val uiState: StateFlow<EpisodeDetailUiState> =
        repository
            .observeEpisode(episodeId)
            .flatMapLatest { episode ->
                if (episode == null) {
                    flowOf(EpisodeDetailUiState(isLoading = false))
                } else {
                    repository.observePodcast(episode.podcastId).map { podcast ->
                        EpisodeDetailUiState(isLoading = false, episode = episode, podcast = podcast)
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), EpisodeDetailUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
