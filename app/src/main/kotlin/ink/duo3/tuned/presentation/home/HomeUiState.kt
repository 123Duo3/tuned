package ink.duo3.tuned.presentation.home

import ink.duo3.tuned.domain.model.PodcastSearchResult
import ink.duo3.tuned.domain.model.RecentEpisode
import ink.duo3.tuned.domain.model.SubscriptionEpisode
import ink.duo3.tuned.domain.player.EpisodePlaybackSnapshot

/**
 * State for the card-based home. [isLoading] covers the first DB read; [subscriptionEpisodes]
 * backs the "Subscribed" row (each subscription's latest episode); [episodePlayback] carries the
 * per-episode button state for home lists; [isPlaying] drives the baseline animation while audio
 * plays. [recentEpisodes] feeds the "Recently Updated" card list.
 * [topCharts] backs the discovery strip — [chartsLoading] shows its own spinner so the rest
 * of the home renders without waiting on the network. Tapping a chart subscribes by its feed
 * URL: [subscribingFeedUrl] marks the in-flight one and [addedPodcastId] signals navigation.
 * [isRefreshing] backs the pull-to-refresh indicator while every feed is being re-fetched.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val subscriptionEpisodes: List<SubscriptionEpisode> = emptyList(),
    val recentEpisodes: List<RecentEpisode> = emptyList(),
    val episodePlayback: Map<String, EpisodePlaybackSnapshot> = emptyMap(),
    val isPlaying: Boolean = false,
    val topCharts: List<PodcastSearchResult> = emptyList(),
    val chartsLoading: Boolean = false,
    val subscribingFeedUrl: String? = null,
    val addedPodcastId: String? = null,
    val isRefreshing: Boolean = false,
)
