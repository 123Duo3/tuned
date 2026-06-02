package ink.duo3.tuned.presentation.home

import ink.duo3.tuned.domain.model.Podcast
import ink.duo3.tuned.domain.model.RecentEpisode

/**
 * State for the card-based home. [isLoading] covers the first DB read; [subscriptions]
 * backs the "Subscribed" card's artwork row; [isPlaying] drives the wordmark's baseline
 * animation while audio plays. [recentEpisodes] feeds the "Recently Updated" card list.
 * More section data (resume) joins this state as those cards land.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val subscriptions: List<Podcast> = emptyList(),
    val recentEpisodes: List<RecentEpisode> = emptyList(),
    val isPlaying: Boolean = false,
)
