package ink.duo3.tuned.presentation.episode

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
import kotlinx.coroutines.flow.StateFlow
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EpisodeDetailViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `maps the episode and its parent podcast once the db emits`() =
        runTest {
            val repo = FakePodcastRepository(episode("e1", "p1"), podcast("p1"))
            val vm = EpisodeDetailViewModel("e1", repo, FakePlaybackController())
            assertTrue(vm.uiState.value.isLoading)

            val job = launch { vm.uiState.collect { it } }
            runCurrent()

            assertFalse(vm.uiState.value.isLoading)
            assertEquals(
                "e1",
                vm.uiState.value.episode
                    ?.id,
            )
            assertEquals(
                "p1",
                vm.uiState.value.podcast
                    ?.id,
            )
            job.cancel()
        }

    @Test
    fun `null episode after load leaves podcast null`() =
        runTest {
            val repo = FakePodcastRepository(episode = null, podcast = null)
            val vm = EpisodeDetailViewModel("missing", repo, FakePlaybackController())
            val job = launch { vm.uiState.collect { it } }
            runCurrent()

            assertFalse(vm.uiState.value.isLoading)
            assertNull(vm.uiState.value.episode)
            assertNull(vm.uiState.value.podcast)
            job.cancel()
        }

    @Test
    fun `play hands the loaded episode to the controller with podcast artwork fallback`() =
        runTest {
            val repo = FakePodcastRepository(episode("e1", "p1"), podcast("p1"))
            val controller = FakePlaybackController()
            val vm = EpisodeDetailViewModel("e1", repo, controller)
            val job = launch { vm.uiState.collect { it } }
            runCurrent()

            vm.play()

            assertEquals("e1", controller.played?.episodeId)
            assertEquals("https://audio/e1", controller.played?.streamUrl)
            assertEquals("art-p1", controller.played?.artworkUrl)
            job.cancel()
        }

    @Test
    fun `play is a no-op when the loaded episode has no audio`() =
        runTest {
            val repo = FakePodcastRepository(episode("e1", "p1", enclosureUrl = null), podcast("p1"))
            val controller = FakePlaybackController()
            val vm = EpisodeDetailViewModel("e1", repo, controller)
            val job = launch { vm.uiState.collect { it } }
            runCurrent()

            vm.play()

            assertNull(controller.played)
            job.cancel()
        }

    private fun podcast(id: String) =
        Podcast(
            id = id,
            feedUrl = "https://feed/$id",
            title = "Title $id",
            author = null,
            description = null,
            artworkUrl = "art-$id",
        )

    private fun episode(
        id: String,
        podcastId: String,
        enclosureUrl: String? = "https://audio/$id",
    ) = Episode(
        id = id,
        podcastId = podcastId,
        title = "Episode $id",
        description = null,
        enclosureUrl = enclosureUrl,
        artworkUrl = null,
        publishedAtMs = 0,
        durationMs = null,
    )

    private class FakePodcastRepository(
        private val episode: Episode?,
        private val podcast: Podcast?,
    ) : PodcastRepository {
        override fun observeSubscriptions(): Flow<List<Podcast>> = flowOf(emptyList())

        override fun observePodcast(podcastId: String): Flow<Podcast?> = flowOf(podcast)

        override fun observeEpisodes(podcastId: String): Flow<List<Episode>> = flowOf(emptyList())

        override fun observeEpisode(episodeId: String): Flow<Episode?> = flowOf(episode)

        override fun observeRecentEpisodes(limit: Int): Flow<List<RecentEpisode>> = flowOf(emptyList())

        override suspend fun subscribe(feedUrl: String): Outcome<String> = error("unused")

        override suspend fun refresh(podcastId: String): Outcome<Unit> = error("unused")
    }

    private class FakePlaybackController : PlaybackController {
        var played: PlayableEpisode? = null
            private set

        override val state: StateFlow<PlaybackState> = MutableStateFlow(PlaybackState())

        override fun play(item: PlayableEpisode) {
            played = item
        }

        override fun resume() = Unit

        override fun pause() = Unit

        override fun seekTo(positionMs: Long) = Unit

        override fun seekBy(deltaMs: Long) = Unit

        override fun setSpeed(speed: Float) = Unit

        override fun stop() = Unit
    }
}
