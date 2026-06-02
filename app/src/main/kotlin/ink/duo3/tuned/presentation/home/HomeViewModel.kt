package ink.duo3.tuned.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.duo3.tuned.domain.player.PlaybackController
import ink.duo3.tuned.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Drives the card-based home. The "Subscribed" section comes from the repository; playback
 * state is folded in so the wordmark can ripple while audio plays. As more cards land
 * (recently updated, resume) they join this single observed state.
 */
class HomeViewModel(
    repository: PodcastRepository,
    playbackController: PlaybackController,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(
            repository.observeSubscriptions(),
            repository.observeRecentEpisodes(),
            playbackController.state,
        ) { subscriptions, recent, playback ->
            HomeUiState(
                isLoading = false,
                subscriptions = subscriptions,
                recentEpisodes = recent,
                isPlaying = playback.isPlaying,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
