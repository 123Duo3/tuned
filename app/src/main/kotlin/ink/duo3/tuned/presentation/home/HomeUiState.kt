package ink.duo3.tuned.presentation.home

import ink.duo3.tuned.domain.model.Podcast
import ink.duo3.tuned.domain.model.PodcastSearchResult
import ink.duo3.tuned.domain.model.RecentEpisode

/**
 * State for the card-based home. [isLoading] covers the first DB read; [subscriptions]
 * backs the "Subscribed" card's artwork row; [isPlaying] drives the wordmark's baseline
 * animation while audio plays. [recentEpisodes] feeds the "Recently Updated" card list.
 * [topCharts] backs the discovery strip — [chartsLoading] shows its own spinner so the rest
 * of the home renders without waiting on the network. Tapping a chart subscribes by its feed
 * URL: [subscribingFeedUrl] marks the in-flight one and [addedPodcastId] signals navigation.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val subscriptions: List<Podcast> = emptyList(),
    val recentEpisodes: List<RecentEpisode> = emptyList(),
    val isPlaying: Boolean = false,
    val topCharts: List<PodcastSearchResult> = emptyList(),
    val chartsLoading: Boolean = false,
    val subscribingFeedUrl: String? = null,
    val addedPodcastId: String? = null,
)
