package ink.duo3.tuned.presentation.player

import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.model.Chapter
import ink.duo3.tuned.domain.model.Episode
import ink.duo3.tuned.domain.model.Podcast
import ink.duo3.tuned.domain.model.RecentEpisode
import ink.duo3.tuned.domain.model.SubscriptionEpisode
import ink.duo3.tuned.domain.player.PlayableEpisode
import ink.duo3.tuned.domain.player.PlaybackController
import ink.duo3.tuned.domain.player.PlaybackState
import ink.duo3.tuned.domain.repository.ChaptersRepository
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `playPause pauses while playing and resumes while paused`() {
        val controller = FakePlaybackController()
        val vm = playerViewModel(controller)

        controller.emit(PlaybackState(episodeId = "e1", isPlaying = true))
        vm.playPause()
        assertEquals(listOf("pause"), controller.calls)

        controller.emit(PlaybackState(episodeId = "e1", isPlaying = false))
        vm.playPause()
        assertEquals(listOf("pause", "resume"), controller.calls)
    }

    @Test
    fun `skip maps to fixed seek deltas`() {
        val controller = FakePlaybackController()
        val vm = playerViewModel(controller)

        vm.skipBack()
        vm.skipForward()

        assertEquals(listOf(-15_000L, 30_000L), controller.seekDeltas)
    }

    @Test
    fun `seekTo delegates to the controller`() {
        val controller = FakePlaybackController()
        val vm = playerViewModel(controller)

        vm.seekTo(42_000L)

        assertEquals(listOf(42_000L), controller.seekPositions)
    }

    @Test
    fun `cycleSpeed steps up then wraps to the first preset`() {
        val controller = FakePlaybackController()
        val vm = playerViewModel(controller)

        controller.emit(PlaybackState(speed = 1f))
        vm.cycleSpeed()
        assertEquals(1.2f, controller.speeds.last())

        controller.emit(PlaybackState(speed = 2f))
        vm.cycleSpeed()
        assertEquals(1f, controller.speeds.last())
    }

    @Test
    fun `startSleepTimer converts preset minutes to milliseconds`() {
        val controller = FakePlaybackController()
        val vm = playerViewModel(controller)

        vm.startSleepTimer(30)

        assertEquals(listOf(30 * 60_000L), controller.sleepDurations)
    }

    @Test
    fun `cancelSleepTimer delegates to the controller`() {
        val controller = FakePlaybackController()
        val vm = playerViewModel(controller)

        vm.cancelSleepTimer()

        assertEquals(listOf("cancelSleepTimer"), controller.calls)
    }

    @Test
    fun `uiState mirrors the controller playback snapshot`() =
        runTest {
            val controller = FakePlaybackController()
            val vm = playerViewModel(controller)
            val job = launch { vm.uiState.collect { it } }

            controller.emit(PlaybackState(episodeId = "e1", title = "Episode One"))
            runCurrent()

            assertEquals("Episode One", vm.uiState.value.playback.title)
            job.cancel()
        }

    @Test
    fun `loads the current episode chapters and tracks the active one by position`() =
        runTest {
            val controller = FakePlaybackController()
            val chapters =
                listOf(
                    Chapter(startTimeMs = 0L, title = "Intro"),
                    Chapter(startTimeMs = 90_000L, title = "Topic"),
                    Chapter(startTimeMs = 132_500L, title = "Outro"),
                )
            val vm =
                playerViewModel(
                    controller,
                    podcast = FakePodcastRepository(episode("e1", chaptersUrl = "https://chapters.json")),
                    chaptersRepo = FakeChaptersRepository(Outcome.Success(chapters)),
                )
            val job = launch { vm.uiState.collect { it } }

            controller.emit(PlaybackState(episodeId = "e1", positionMs = 100_000L))
            runCurrent()

            assertEquals(
                listOf("Intro", "Topic", "Outro"),
                vm.uiState.value.chapters
                    .map { it.title },
            )
            assertEquals(1, vm.uiState.value.currentChapterIndex)
            assertEquals(
                "Topic",
                vm.uiState.value.currentChapter
                    ?.title,
            )
            job.cancel()
        }

    @Test
    fun `an episode without a chapters url has no chapters`() =
        runTest {
            val controller = FakePlaybackController()
            val vm =
                playerViewModel(
                    controller,
                    podcast = FakePodcastRepository(episode("e1", chaptersUrl = null)),
                )
            val job = launch { vm.uiState.collect { it } }

            controller.emit(PlaybackState(episodeId = "e1", positionMs = 1_000L))
            runCurrent()

            assertEquals(
                emptyList<String>(),
                vm.uiState.value.chapters
                    .map { it.title },
            )
            assertNull(vm.uiState.value.currentChapterIndex)
            job.cancel()
        }

    private fun playerViewModel(
        controller: FakePlaybackController,
        podcast: PodcastRepository = FakePodcastRepository(episode = null),
        chaptersRepo: ChaptersRepository = FakeChaptersRepository(Outcome.Success(emptyList())),
    ) = PlayerViewModel(controller, podcast, chaptersRepo)

    private fun episode(
        id: String,
        chaptersUrl: String?,
    ) = Episode(
        id = id,
        podcastId = "p1",
        title = "Episode",
        description = null,
        enclosureUrl = "https://audio.mp3",
        artworkUrl = null,
        publishedAtMs = 0L,
        durationMs = null,
        chaptersUrl = chaptersUrl,
    )

    private class FakeChaptersRepository(
        private val result: Outcome<List<Chapter>>,
    ) : ChaptersRepository {
        override suspend fun chapters(episode: Episode): Outcome<List<Chapter>> = result
    }

    private class FakePodcastRepository(
        private val episode: Episode?,
    ) : PodcastRepository {
        override fun observeSubscriptions(): Flow<List<Podcast>> = flowOf(emptyList())

        override fun observePodcast(podcastId: String): Flow<Podcast?> = flowOf(null)

        override fun observeEpisodes(podcastId: String): Flow<List<Episode>> = flowOf(emptyList())

        override fun observeEpisode(episodeId: String): Flow<Episode?> = flowOf(episode)

        override fun observeRecentEpisodes(limit: Int): Flow<List<RecentEpisode>> = flowOf(emptyList())

        override fun observeSubscriptionEpisodes(): Flow<List<SubscriptionEpisode>> = flowOf(emptyList())

        override suspend fun subscribe(feedUrl: String): Outcome<String> = Outcome.Success("p1")

        override suspend fun refresh(podcastId: String): Outcome<Unit> = Outcome.Success(Unit)

        override suspend fun refreshAll(): List<Outcome<Unit>> = emptyList()
    }

    private class FakePlaybackController : PlaybackController {
        private val _state = MutableStateFlow(PlaybackState())
        override val state: StateFlow<PlaybackState> = _state
        override val audioLevelBars: StateFlow<List<Float>> = MutableStateFlow(emptyList())

        val calls = mutableListOf<String>()
        val seekPositions = mutableListOf<Long>()
        val seekDeltas = mutableListOf<Long>()
        val speeds = mutableListOf<Float>()
        val sleepDurations = mutableListOf<Long>()

        fun emit(state: PlaybackState) {
            _state.value = state
        }

        override fun play(item: PlayableEpisode) {
            calls += "play"
        }

        override fun resume() {
            calls += "resume"
        }

        override fun pause() {
            calls += "pause"
        }

        override fun seekTo(positionMs: Long) {
            seekPositions += positionMs
        }

        override fun seekBy(deltaMs: Long) {
            seekDeltas += deltaMs
        }

        override fun setSpeed(speed: Float) {
            speeds += speed
        }

        override fun stop() {
            calls += "stop"
        }

        override fun startSleepTimer(durationMs: Long) {
            sleepDurations += durationMs
        }

        override fun cancelSleepTimer() {
            calls += "cancelSleepTimer"
        }
    }
}
