package ink.duo3.tuned.presentation.home

import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.model.Episode
import ink.duo3.tuned.domain.model.EpisodeProgress
import ink.duo3.tuned.domain.model.Podcast
import ink.duo3.tuned.domain.model.PodcastSearchResult
import ink.duo3.tuned.domain.model.RecentEpisode
import ink.duo3.tuned.domain.model.SubscriptionEpisode
import ink.duo3.tuned.domain.player.EpisodePlaybackStatus
import ink.duo3.tuned.domain.player.PlayableEpisode
import ink.duo3.tuned.domain.player.PlaybackController
import ink.duo3.tuned.domain.player.PlaybackState
import ink.duo3.tuned.domain.repository.ChartsRepository
import ink.duo3.tuned.domain.repository.PodcastRepository
import ink.duo3.tuned.domain.repository.ProgressRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `starts loading then maps subscriptions once the db emits`() =
        runTest {
            val repo = FakePodcastRepository(listOf(subscriptionEpisode("p1"), subscriptionEpisode("p2")))
            val vm = HomeViewModel(repo, FakeChartsRepository(), FakeProgressRepository(), FakePlaybackController())
            assertTrue(vm.uiState.value.isLoading)

            val job = launch { vm.uiState.collect { it } }
            runCurrent()

            assertFalse(vm.uiState.value.isLoading)
            val ids =
                vm.uiState.value.subscriptionEpisodes
                    .map { it.podcastId }
            assertEquals(listOf("p1", "p2"), ids)
            job.cancel()
        }

    @Test
    fun `folds playback into the state so the wordmark can animate`() =
        runTest {
            val playback = FakePlaybackController()
            val vm = HomeViewModel(FakePodcastRepository(), FakeChartsRepository(), FakeProgressRepository(), playback)

            val job = launch { vm.uiState.collect { it } }
            runCurrent()
            assertFalse(vm.uiState.value.isPlaying)

            playback.emit(PlaybackState(episodeId = "e1", isPlaying = true))
            runCurrent()
            assertTrue(vm.uiState.value.isPlaying)
            job.cancel()
        }

    @Test
    fun `includes recently updated episodes in the state`() =
        runTest {
            val recent = listOf(recentEpisode("e1"), recentEpisode("e2"))
            val vm =
                HomeViewModel(
                    FakePodcastRepository(recent = recent),
                    FakeChartsRepository(),
                    FakeProgressRepository(),
                    FakePlaybackController(),
                )

            val job = launch { vm.uiState.collect { it } }
            runCurrent()

            assertEquals(
                listOf("e1", "e2"),
                vm.uiState.value.recentEpisodes
                    .map { it.id },
            )
            job.cancel()
        }

    @Test
    fun `maps saved episode progress into home play button state`() =
        runTest {
            val latest = listOf(subscriptionEpisode("p1", durationMs = 100_000L))
            val progress = EpisodeProgress("e-p1", positionMs = 25_000L, completed = false, lastPlayedAt = 1L)
            val vm =
                HomeViewModel(
                    FakePodcastRepository(latest),
                    FakeChartsRepository(),
                    FakeProgressRepository(mapOf("e-p1" to progress)),
                    FakePlaybackController(),
                )

            val job = launch { vm.uiState.collect { it } }
            runCurrent()

            val playback =
                vm.uiState.value.episodePlayback
                    .getValue("e-p1")
            assertEquals(EpisodePlaybackStatus.Resume, playback.status)
            assertEquals(0.25f, playback.progress)
            assertEquals(75_000L, playback.remainingMs)
            job.cancel()
        }

    @Test
    fun `refresh drives the indicator and re-fetches every feed`() =
        runTest {
            val repo = FakePodcastRepository(listOf(subscriptionEpisode("p1"), subscriptionEpisode("p2")))
            val gate = CompletableDeferred<Unit>()
            repo.refreshGate = gate
            val vm = HomeViewModel(repo, FakeChartsRepository(), FakeProgressRepository(), FakePlaybackController())

            val job = launch { vm.uiState.collect { it } }
            runCurrent()
            assertFalse(vm.uiState.value.isRefreshing)

            vm.refresh()
            runCurrent()
            assertTrue(vm.uiState.value.isRefreshing)
            assertEquals(1, repo.refreshAllCount)

            // A second pull while one is in flight is a no-op.
            vm.refresh()
            runCurrent()
            assertEquals(1, repo.refreshAllCount)

            gate.complete(Unit)
            runCurrent()
            assertFalse(vm.uiState.value.isRefreshing)
            job.cancel()
        }

    private fun subscriptionEpisode(
        podcastId: String,
        durationMs: Long? = null,
    ) = SubscriptionEpisode(
        podcastId = podcastId,
        podcastTitle = "Title $podcastId",
        podcastArtworkUrl = null,
        episodeId = "e-$podcastId",
        title = "Episode",
        description = null,
        artworkUrl = null,
        enclosureUrl = null,
        publishedAtMs = 1,
        durationMs = durationMs,
    )

    private fun recentEpisode(id: String) =
        RecentEpisode(
            id = id,
            podcastId = "p1",
            title = "Episode $id",
            description = null,
            enclosureUrl = "https://audio/$id",
            artworkUrl = null,
            publishedAtMs = 1,
            durationMs = null,
            podcastTitle = "Podcast",
            podcastArtworkUrl = "https://artwork",
        )

    private class FakePodcastRepository(
        latest: List<SubscriptionEpisode> = emptyList(),
        private val recent: List<RecentEpisode> = emptyList(),
    ) : PodcastRepository {
        private val subscriptionEpisodes = MutableStateFlow(latest)

        override fun observeSubscriptions(): Flow<List<Podcast>> = flowOf(emptyList())

        override fun observePodcast(podcastId: String): Flow<Podcast?> = flowOf(null)

        override fun observeEpisodes(podcastId: String): Flow<List<Episode>> = flowOf(emptyList())

        override fun observeEpisode(episodeId: String): Flow<Episode?> = flowOf(null)

        override fun observeRecentEpisodes(limit: Int): Flow<List<RecentEpisode>> = flowOf(recent)

        override fun observeSubscriptionEpisodes(): Flow<List<SubscriptionEpisode>> = subscriptionEpisodes

        override suspend fun subscribe(feedUrl: String): Outcome<String> = error("unused")

        override suspend fun refresh(podcastId: String): Outcome<Unit> = error("unused")

        var refreshAllCount = 0
            private set

        // When set, refreshAll suspends on it so a test can observe the in-flight indicator.
        var refreshGate: CompletableDeferred<Unit>? = null

        override suspend fun refreshAll(): List<Outcome<Unit>> {
            refreshAllCount++
            refreshGate?.await()
            return subscriptionEpisodes.value.map { Outcome.Success(Unit) }
        }
    }

    private class FakeProgressRepository(
        private val progressByEpisodeId: Map<String, EpisodeProgress> = emptyMap(),
    ) : ProgressRepository {
        override suspend fun resumePositionMs(episodeId: String): Long = 0L

        override suspend fun save(
            episodeId: String,
            positionMs: Long,
            completed: Boolean,
        ) = Unit

        override fun observe(episodeId: String): Flow<EpisodeProgress?> = flowOf(progressByEpisodeId[episodeId])
    }

    private class FakePlaybackController : PlaybackController {
        private val _state = MutableStateFlow(PlaybackState())
        override val state = _state
        override val audioLevelBars = MutableStateFlow(emptyList<Float>())

        fun emit(value: PlaybackState) {
            _state.value = value
        }

        override fun play(item: PlayableEpisode) = Unit

        override fun resume() = Unit

        override fun pause() = Unit

        override fun seekTo(positionMs: Long) = Unit

        override fun seekBy(deltaMs: Long) = Unit

        override fun setSpeed(speed: Float) = Unit

        override fun stop() = Unit

        override fun startSleepTimer(durationMs: Long) = Unit

        override fun cancelSleepTimer() = Unit
    }

    private class FakeChartsRepository : ChartsRepository {
        override suspend fun topPodcasts(
            country: String,
            genreId: Int?,
        ): Outcome<List<PodcastSearchResult>> = Outcome.Success(emptyList())
    }
}
