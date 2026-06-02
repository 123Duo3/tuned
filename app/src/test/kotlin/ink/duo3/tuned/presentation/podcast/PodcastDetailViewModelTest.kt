package ink.duo3.tuned.presentation.podcast

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.model.Episode
import ink.duo3.tuned.domain.model.Podcast
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PodcastDetailViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `maps the podcast and its episodes once the db emits`() =
        runTest {
            val repo = FakePodcastRepository(podcast("p1"), listOf(episode("e1"), episode("e2")))
            val vm = PodcastDetailViewModel("p1", repo)
            assertTrue(vm.uiState.value.isLoading)

            val job = launch { vm.uiState.collect { it } }
            runCurrent()

            assertFalse(vm.uiState.value.isLoading)
            assertEquals(
                "p1",
                vm.uiState.value.podcast
                    ?.id,
            )
            assertEquals(
                listOf("e1", "e2"),
                vm.uiState.value.episodes
                    .map { it.id },
            )
            job.cancel()
        }

    @Test
    fun `null podcast after load signals an unsubscribed id`() =
        runTest {
            val repo = FakePodcastRepository(podcast = null, episodes = emptyList())
            val vm = PodcastDetailViewModel("missing", repo)
            val job = launch { vm.uiState.collect { it } }
            runCurrent()

            assertFalse(vm.uiState.value.isLoading)
            assertNull(vm.uiState.value.podcast)
            job.cancel()
        }

    @Test
    fun `refresh failure surfaces the error and consumeError clears it`() =
        runTest {
            val repo = FakePodcastRepository(podcast("p1"), emptyList())
            repo.refreshOutcome = Outcome.Failure(AppError.Network())
            val vm = PodcastDetailViewModel("p1", repo)
            val job = launch { vm.uiState.collect { it } }
            runCurrent()

            vm.refresh()
            runCurrent()
            assertTrue(vm.uiState.value.refreshError is AppError.Network)
            assertFalse(vm.uiState.value.isRefreshing)

            vm.consumeError()
            runCurrent()
            assertNull(vm.uiState.value.refreshError)
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

    private fun episode(id: String) =
        Episode(
            id = id,
            podcastId = "p1",
            title = "Episode $id",
            description = null,
            enclosureUrl = null,
            publishedAtMs = 0,
            durationMs = null,
        )

    private class FakePodcastRepository(
        podcast: Podcast?,
        episodes: List<Episode>,
    ) : PodcastRepository {
        private val podcast = MutableStateFlow(podcast)
        private val episodes = MutableStateFlow(episodes)
        var refreshOutcome: Outcome<Unit> = Outcome.Success(Unit)

        override fun observeSubscriptions(): Flow<List<Podcast>> = flowOf(emptyList())

        override fun observePodcast(podcastId: String): Flow<Podcast?> = podcast

        override fun observeEpisodes(podcastId: String): Flow<List<Episode>> = episodes

        override fun observeEpisode(episodeId: String): Flow<Episode?> = flowOf(null)

        override suspend fun subscribe(feedUrl: String): Outcome<String> = error("unused")

        override suspend fun refresh(podcastId: String): Outcome<Unit> = refreshOutcome
    }
}
