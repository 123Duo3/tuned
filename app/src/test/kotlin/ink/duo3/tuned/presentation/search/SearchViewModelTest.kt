package ink.duo3.tuned.presentation.search

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.model.Episode
import ink.duo3.tuned.domain.model.Podcast
import ink.duo3.tuned.domain.model.PodcastSearchResult
import ink.duo3.tuned.domain.model.RecentEpisode
import ink.duo3.tuned.domain.repository.PodcastRepository
import ink.duo3.tuned.domain.repository.SearchRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
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
    fun `keyword query runs a debounced search`() =
        runTest {
            val search = FakeSearchRepository(Outcome.Success(listOf(RESULT)))
            val vm = SearchViewModel(search, FakePodcastRepository())

            vm.onQueryChange("design")
            advanceTimeBy(DEBOUNCE)
            runCurrent()

            assertEquals(listOf("design"), search.terms)
            assertEquals(listOf(RESULT), vm.uiState.value.results)
            assertFalse(vm.uiState.value.isSearching)
        }

    @Test
    fun `rapid typing only searches the final term`() =
        runTest {
            val search = FakeSearchRepository(Outcome.Success(emptyList()))
            val vm = SearchViewModel(search, FakePodcastRepository())

            vm.onQueryChange("d")
            advanceTimeBy(100)
            vm.onQueryChange("de")
            advanceTimeBy(100)
            vm.onQueryChange("design")
            advanceUntilIdle()

            assertEquals(listOf("design"), search.terms)
        }

    @Test
    fun `a feed-url query flags isUrlQuery and skips search`() =
        runTest {
            val search = FakeSearchRepository(Outcome.Success(emptyList()))
            val vm = SearchViewModel(search, FakePodcastRepository())

            vm.onQueryChange("https://feed.example/rss")
            advanceUntilIdle()

            assertTrue(vm.uiState.value.isUrlQuery)
            assertEquals(emptyList<String>(), search.terms)
        }

    @Test
    fun `search failure surfaces error until consumed`() =
        runTest {
            val search = FakeSearchRepository(Outcome.Failure(AppError.Network()))
            val vm = SearchViewModel(search, FakePodcastRepository())

            vm.onQueryChange("design")
            advanceUntilIdle()

            assertTrue(vm.uiState.value.error is AppError.Network)
            vm.consumeError()
            assertNull(vm.uiState.value.error)
        }

    @Test
    fun `subscribing to a tapped result marks that feed and surfaces the podcast id`() =
        runTest {
            val podcast = FakePodcastRepository()
            val vm = SearchViewModel(FakeSearchRepository(Outcome.Success(emptyList())), podcast)

            vm.subscribe(RESULT.feedUrl)
            runCurrent()

            assertEquals(listOf(RESULT.feedUrl), podcast.requests)
            assertEquals(PODCAST_ID, vm.uiState.value.addedPodcastId)
            assertNull(vm.uiState.value.subscribingFeedUrl)

            vm.consumeAdded()
            assertNull(vm.uiState.value.addedPodcastId)
        }

    @Test
    fun `subscribing to a typed url uses the current query`() =
        runTest {
            val podcast = FakePodcastRepository()
            val vm = SearchViewModel(FakeSearchRepository(Outcome.Success(emptyList())), podcast)

            vm.onQueryChange("  https://feed.example/rss  ")
            vm.subscribe()
            runCurrent()

            assertEquals(listOf("https://feed.example/rss"), podcast.requests)
        }

    @Test
    fun `second subscribe while one is in flight does not start another request`() =
        runTest {
            val gate = CompletableDeferred<Outcome<String>>()
            val podcast = FakePodcastRepository { gate.await() }
            val vm = SearchViewModel(FakeSearchRepository(Outcome.Success(emptyList())), podcast)

            vm.subscribe(RESULT.feedUrl)
            runCurrent()
            vm.subscribe(RESULT.feedUrl)
            runCurrent()

            assertEquals(1, podcast.requests.size)
            gate.complete(Outcome.Success(PODCAST_ID))
            runCurrent()
            assertNull(vm.uiState.value.subscribingFeedUrl)
        }

    @Test
    fun `looksLikeFeedUrl distinguishes urls from search terms`() {
        assertTrue(looksLikeFeedUrl("https://feed.example/rss"))
        assertTrue(looksLikeFeedUrl("example.com/feed"))
        assertFalse(looksLikeFeedUrl("design"))
        assertFalse(looksLikeFeedUrl("the daily"))
        assertFalse(looksLikeFeedUrl(""))
    }

    private class FakeSearchRepository(
        private val outcome: Outcome<List<PodcastSearchResult>>,
    ) : SearchRepository {
        val terms = mutableListOf<String>()

        override suspend fun searchPodcasts(term: String): Outcome<List<PodcastSearchResult>> {
            terms += term
            return outcome
        }
    }

    private class FakePodcastRepository(
        private val subscribe: suspend () -> Outcome<String> = { Outcome.Success(PODCAST_ID) },
    ) : PodcastRepository {
        val requests = mutableListOf<String>()

        override fun observeSubscriptions(): Flow<List<Podcast>> = flowOf(emptyList())

        override fun observePodcast(podcastId: String): Flow<Podcast?> = flowOf(null)

        override fun observeEpisodes(podcastId: String): Flow<List<Episode>> = flowOf(emptyList())

        override fun observeEpisode(episodeId: String): Flow<Episode?> = flowOf(null)

        override fun observeRecentEpisodes(limit: Int): Flow<List<RecentEpisode>> = flowOf(emptyList())

        override suspend fun subscribe(feedUrl: String): Outcome<String> {
            requests += feedUrl
            return subscribe()
        }

        override suspend fun refresh(podcastId: String): Outcome<Unit> = error("unused")

        override suspend fun refreshAll(): List<Outcome<Unit>> = error("unused")
    }

    private companion object {
        const val PODCAST_ID = "podcast-id"
        const val DEBOUNCE = 400L
        val RESULT =
            PodcastSearchResult(
                feedUrl = "https://feed.example/rss",
                title = "Design Matters",
                author = "Designer",
                artworkUrl = "https://img.example/art.jpg",
                episodeCount = 42,
            )
    }
}
