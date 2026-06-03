package ink.duo3.tuned.presentation.home

import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.model.Episode
import ink.duo3.tuned.domain.model.Podcast
import ink.duo3.tuned.domain.model.RecentEpisode
import ink.duo3.tuned.domain.player.PlayableEpisode
import ink.duo3.tuned.domain.player.PlaybackController
import ink.duo3.tuned.domain.player.PlaybackState
import ink.duo3.tuned.domain.repository.PodcastRepository
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
            val repo = FakePodcastRepository(listOf(podcast("p1"), podcast("p2")))
            val vm = HomeViewModel(repo, FakePlaybackController())
            assertTrue(vm.uiState.value.isLoading)

            val job = launch { vm.uiState.collect { it } }
            runCurrent()

            assertFalse(vm.uiState.value.isLoading)
            val ids =
                vm.uiState.value.subscriptions
                    .map { it.id }
            assertEquals(listOf("p1", "p2"), ids)
            job.cancel()
        }

    @Test
    fun `folds playback into the state so the wordmark can animate`() =
        runTest {
            val playback = FakePlaybackController()
            val vm = HomeViewModel(FakePodcastRepository(), playback)

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
            val vm = HomeViewModel(FakePodcastRepository(recent = recent), FakePlaybackController())

            val job = launch { vm.uiState.collect { it } }
            runCurrent()

            assertEquals(
                listOf("e1", "e2"),
                vm.uiState.value.recentEpisodes
                    .map { it.id },
            )
            job.cancel()
        }

    private fun podcast(id: String) =
        Podcast(
            id = id,
            feedUrl = "https://feed/$id",
            title = "Title $id",
            author = null,
            description = null,
            artworkUrl = null,
        )

    private fun recentEpisode(id: String) =
        RecentEpisode(
            id = id,
            podcastId = "p1",
            title = "Episode $id",
            artworkUrl = null,
            publishedAtMs = 1,
            durationMs = null,
            podcastTitle = "Podcast",
            podcastArtworkUrl = "https://artwork",
        )

    private class FakePodcastRepository(
        initial: List<Podcast> = emptyList(),
        private val recent: List<RecentEpisode> = emptyList(),
    ) : PodcastRepository {
        private val subscriptions = MutableStateFlow(initial)

        override fun observeSubscriptions(): Flow<List<Podcast>> = subscriptions

        override fun observePodcast(podcastId: String): Flow<Podcast?> = flowOf(null)

        override fun observeEpisodes(podcastId: String): Flow<List<Episode>> = flowOf(emptyList())

        override fun observeEpisode(episodeId: String): Flow<Episode?> = flowOf(null)

        override fun observeRecentEpisodes(limit: Int): Flow<List<RecentEpisode>> = flowOf(recent)

        override suspend fun subscribe(feedUrl: String): Outcome<String> = error("unused")

        override suspend fun refresh(podcastId: String): Outcome<Unit> = error("unused")
    }

    private class FakePlaybackController : PlaybackController {
        private val _state = MutableStateFlow(PlaybackState())
        override val state = _state

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
}
