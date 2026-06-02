package ink.duo3.tuned.presentation.episode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.duo3.tuned.domain.player.PlayableEpisode
import ink.duo3.tuned.domain.player.PlaybackController
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
    private val playbackController: PlaybackController,
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

    /**
     * Starts playback of the loaded episode. No-op until the episode has resolved and has
     * audio; artwork falls back to the podcast's. The resume position is resolved in the
     * playback layer, so this only needs to hand over the stream and display metadata.
     */
    fun play() {
        val state = uiState.value
        val episode = state.episode ?: return
        val streamUrl = episode.enclosureUrl ?: return
        playbackController.play(
            PlayableEpisode(
                episodeId = episode.id,
                title = episode.title.orEmpty(),
                podcastTitle = state.podcast?.title.orEmpty(),
                artworkUrl = episode.artworkUrl ?: state.podcast?.artworkUrl,
                streamUrl = streamUrl,
            ),
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
