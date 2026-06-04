package ink.duo3.tuned.presentation.library

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.model.Episode
import ink.duo3.tuned.domain.model.Podcast
import ink.duo3.tuned.domain.model.RecentEpisode
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
class LibraryViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `maps subscriptions and clears loading once the db emits`() =
        runTest {
            val repo = FakePodcastRepository(listOf(podcast("p1")))
            val vm = LibraryViewModel(repo)
            assertTrue(vm.uiState.value.isLoading)

            val job = launch { vm.uiState.collect { it } }
            runCurrent()

            assertFalse(vm.uiState.value.isLoading)
            val ids =
                vm.uiState.value.podcasts
                    .map { it.id }
            assertEquals(listOf("p1"), ids)
            job.cancel()
        }

    @Test
    fun `refresh failure surfaces the error and consumeError clears it`() =
        runTest {
            val repo = FakePodcastRepository(listOf(podcast("p1")))
            repo.refreshOutcome = Outcome.Failure(AppError.Network())
            val vm = LibraryViewModel(repo)
            val job = launch { vm.uiState.collect { it } }
            runCurrent()

            vm.refresh("p1")
            runCurrent()
            assertTrue(vm.uiState.value.refreshError is AppError.Network)
            assertFalse("p1" in vm.uiState.value.refreshingIds)

            vm.consumeError()
            runCurrent()
            assertNull(vm.uiState.value.refreshError)
            job.cancel()
        }

    @Test
    fun `refresh success records no error and stops marking the podcast`() =
        runTest {
            val repo = FakePodcastRepository(listOf(podcast("p1")))
            val vm = LibraryViewModel(repo)
            val job = launch { vm.uiState.collect { it } }
            runCurrent()

            vm.refresh("p1")
            runCurrent()

            assertNull(vm.uiState.value.refreshError)
            assertFalse("p1" in vm.uiState.value.refreshingIds)
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

    private class FakePodcastRepository(
        initial: List<Podcast> = emptyList(),
    ) : PodcastRepository {
        private val subscriptions = MutableStateFlow(initial)
        var refreshOutcome: Outcome<Unit> = Outcome.Success(Unit)

        override fun observeSubscriptions(): Flow<List<Podcast>> = subscriptions

        override fun observePodcast(podcastId: String): Flow<Podcast?> = flowOf(null)

        override fun observeEpisodes(podcastId: String): Flow<List<Episode>> = flowOf(emptyList())

        override fun observeEpisode(episodeId: String): Flow<Episode?> = flowOf(null)

        override fun observeRecentEpisodes(limit: Int): Flow<List<RecentEpisode>> = flowOf(emptyList())

        override suspend fun subscribe(feedUrl: String): Outcome<String> = error("unused")

        override suspend fun refresh(podcastId: String): Outcome<Unit> = refreshOutcome

        override suspend fun refreshAll(): List<Outcome<Unit>> = error("unused")
    }
}
