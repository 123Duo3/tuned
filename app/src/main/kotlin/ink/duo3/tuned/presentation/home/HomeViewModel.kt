package ink.duo3.tuned.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.model.PodcastSearchResult
import ink.duo3.tuned.domain.model.SubscriptionEpisode
import ink.duo3.tuned.domain.player.PlayableEpisode
import ink.duo3.tuned.domain.player.PlaybackController
import ink.duo3.tuned.domain.repository.ChartsRepository
import ink.duo3.tuned.domain.repository.PodcastRepository
import ink.duo3.tuned.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Drives the card-based home. The "Subscribed" and "Recently Updated" sections come from the
 * repository; playback state is folded in so the wordmark can ripple while audio plays. Top
 * Charts is loaded once (network) into its own flow so a slow/failed fetch never blocks the
 * rest of the home. Tapping a chart subscribes by feed URL, then the screen navigates to the
 * new podcast. The screen collects exactly one [HomeUiState].
 */
class HomeViewModel(
    private val repository: PodcastRepository,
    private val chartsRepository: ChartsRepository,
    private val progressRepository: ProgressRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {
    private val charts = MutableStateFlow(ChartsSection())
    private val subscribing = MutableStateFlow(SubscribeSection())

    val uiState: StateFlow<HomeUiState> =
        combine(
            repository.observeSubscriptionEpisodes(),
            repository.observeRecentEpisodes(),
            playbackController.state,
            charts,
            subscribing,
        ) { subscriptionEpisodes, recent, playback, chartsSection, subscribeSection ->
            HomeUiState(
                isLoading = false,
                subscriptionEpisodes = subscriptionEpisodes,
                recentEpisodes = recent,
                isPlaying = playback.isPlaying,
                topCharts = chartsSection.results,
                chartsLoading = chartsSection.loading,
                subscribingFeedUrl = subscribeSection.subscribingFeedUrl,
                addedPodcastId = subscribeSection.addedPodcastId,
                isRefreshing = subscribeSection.refreshing,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    init {
        loadCharts()
    }

    private fun loadCharts() {
        charts.update { it.copy(loading = true) }
        viewModelScope.launch {
            val results =
                when (val outcome = chartsRepository.topPodcasts(deviceCountry())) {
                    is Outcome.Success -> outcome.value
                    is Outcome.Failure -> emptyList()
                }
            charts.value = ChartsSection(loading = false, results = results)
        }
    }

    /** Subscribes to a tapped chart entry's [feedUrl], then signals navigation via [HomeUiState]. */
    fun subscribe(feedUrl: String) {
        if (subscribing.value.subscribingFeedUrl != null) return
        subscribing.update { it.copy(subscribingFeedUrl = feedUrl) }
        viewModelScope.launch {
            subscribing.value =
                when (val outcome = repository.subscribe(feedUrl)) {
                    is Outcome.Success -> SubscribeSection(addedPodcastId = outcome.value)
                    is Outcome.Failure -> SubscribeSection()
                }
        }
    }

    fun consumeAdded() = subscribing.update { it.copy(addedPodcastId = null) }

    /** Starts playback of a subscription's latest episode from its saved resume position. No-op without audio. */
    fun play(episode: SubscriptionEpisode) {
        val streamUrl = episode.enclosureUrl ?: return
        playbackController.play(
            PlayableEpisode(
                episodeId = episode.episodeId,
                title = episode.title.orEmpty(),
                podcastTitle = episode.podcastTitle.orEmpty(),
                artworkUrl = episode.artworkUrl ?: episode.podcastArtworkUrl,
                streamUrl = streamUrl,
            ),
        )
    }

    /** Marks an episode finished (position 0, completed) from the card's overflow menu. */
    fun markPlayed(episodeId: String) {
        viewModelScope.launch { progressRepository.save(episodeId, positionMs = 0L, completed = true) }
    }

    /**
     * Pull-to-refresh: re-fetches every subscription. Per-feed failures are isolated inside
     * [PodcastRepository.refreshAll], and the Room flows push any new episodes into the state
     * on their own, so here we only drive the indicator. Ignored while already refreshing.
     */
    fun refresh() {
        if (subscribing.value.refreshing) return
        subscribing.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            repository.refreshAll()
            subscribing.update { it.copy(refreshing = false) }
        }
    }

    private fun deviceCountry(): String = Locale.getDefault().country.ifBlank { DEFAULT_COUNTRY }

    private data class ChartsSection(
        val loading: Boolean = false,
        val results: List<PodcastSearchResult> = emptyList(),
    )

    private data class SubscribeSection(
        val subscribingFeedUrl: String? = null,
        val addedPodcastId: String? = null,
        val refreshing: Boolean = false,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val DEFAULT_COUNTRY = "US"
    }
}
