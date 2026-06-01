package ink.duo3.tuned.presentation.search

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.model.Episode
import ink.duo3.tuned.domain.model.Podcast
import ink.duo3.tuned.domain.repository.PodcastRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
class SearchViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `blank query does not subscribe`() =
        runTest {
            val repo = FakePodcastRepository()
            val vm = SearchViewModel(repo)

            vm.onQueryChange("   ")
            vm.subscribe()
            runCurrent()

            assertEquals(emptyList<String>(), repo.requests)
            assertFalse(vm.uiState.value.isSubmitting)
        }

    @Test
    fun `success clears query and surfaces podcast id until consumed`() =
        runTest {
            val repo = FakePodcastRepository()
            val vm = SearchViewModel(repo)

            vm.onQueryChange("  https://feed.example/rss  ")
            vm.subscribe()
            runCurrent()

            assertEquals(listOf("https://feed.example/rss"), repo.requests)
            assertEquals("", vm.uiState.value.query)
            assertEquals(PODCAST_ID, vm.uiState.value.addedPodcastId)
            assertFalse(vm.uiState.value.isSubmitting)

            vm.consumeAdded()
            assertNull(vm.uiState.value.addedPodcastId)
        }

    @Test
    fun `failure surfaces error until consumed`() =
        runTest {
            val repo = FakePodcastRepository(Outcome.Failure(AppError.InvalidUrl()))
            val vm = SearchViewModel(repo)

            vm.onQueryChange("https://feed.example/rss")
            vm.subscribe()
            runCurrent()

            assertTrue(vm.uiState.value.error is AppError.InvalidUrl)
            assertFalse(vm.uiState.value.isSubmitting)

            vm.consumeError()
            assertNull(vm.uiState.value.error)
        }

    @Test
    fun `second tap while subscribing does not start another request`() =
        runTest {
            val result = CompletableDeferred<Outcome<String>>()
            val repo = FakePodcastRepository { result.await() }
            val vm = SearchViewModel(repo)

            vm.onQueryChange("https://feed.example/rss")
            vm.subscribe()
            runCurrent()
            vm.subscribe()
            runCurrent()

            assertEquals(1, repo.requests.size)
            result.complete(Outcome.Success(PODCAST_ID))
            runCurrent()
            assertFalse(vm.uiState.value.isSubmitting)
        }

    private class FakePodcastRepository(
        private val subscribe: suspend () -> Outcome<String> = { Outcome.Success(PODCAST_ID) },
    ) : PodcastRepository {
        constructor(outcome: Outcome<String>) : this({ outcome })

        val requests = mutableListOf<String>()

        override fun observeSubscriptions(): Flow<List<Podcast>> = flowOf(emptyList())

        override fun observePodcast(podcastId: String): Flow<Podcast?> = flowOf(null)

        override fun observeEpisodes(podcastId: String): Flow<List<Episode>> = flowOf(emptyList())

        override suspend fun subscribe(feedUrl: String): Outcome<String> {
            requests += feedUrl
            return subscribe()
        }

        override suspend fun refresh(podcastId: String): Outcome<Unit> = error("unused")
    }

    private companion object {
        const val PODCAST_ID = "podcast-id"
    }
}
