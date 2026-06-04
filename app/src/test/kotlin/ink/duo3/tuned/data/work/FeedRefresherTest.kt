package ink.duo3.tuned.data.work

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.model.Episode
import ink.duo3.tuned.domain.model.Podcast
import ink.duo3.tuned.domain.model.RecentEpisode
import ink.duo3.tuned.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedRefresherTest {
    private fun podcast(id: String) =
        Podcast(
            id = id,
            feedUrl = "https://example.com/$id.xml",
            title = id,
            author = null,
            description = null,
            artworkUrl = null,
        )

    @Test
    fun `refreshes every subscription and reports the tally`() =
        runTest {
            val repository =
                FakePodcastRepository(
                    subscriptions = listOf(podcast("a"), podcast("b"), podcast("c")),
                )
            val summary = FeedRefresher(repository).refreshAll()

            assertEquals(setOf("a", "b", "c"), repository.refreshedIds)
            assertEquals(
                FeedRefresher.Summary(total = 3, succeeded = 3, retryableFailures = 0, permanentFailures = 0),
                summary,
            )
            assertEquals(0, summary.failed)
            assertFalse(summary.shouldRetry)
        }

    @Test
    fun `a single broken feed does not fail the run`() =
        runTest {
            val repository =
                FakePodcastRepository(
                    subscriptions = listOf(podcast("a"), podcast("b")),
                    failures = mapOf("b" to AppError.Network()),
                )
            val summary = FeedRefresher(repository).refreshAll()

            assertEquals(
                FeedRefresher.Summary(total = 2, succeeded = 1, retryableFailures = 1, permanentFailures = 0),
                summary,
            )
            assertFalse(summary.shouldRetry)
        }

    @Test
    fun `a whole-run wipeout of transient errors asks for a retry`() =
        runTest {
            val repository =
                FakePodcastRepository(
                    subscriptions = listOf(podcast("a"), podcast("b")),
                    failures = mapOf("a" to AppError.Network(), "b" to AppError.Http(503)),
                )
            val summary = FeedRefresher(repository).refreshAll()

            assertEquals(
                FeedRefresher.Summary(total = 2, succeeded = 0, retryableFailures = 2, permanentFailures = 0),
                summary,
            )
            assertTrue(summary.shouldRetry)
        }

    @Test
    fun `a whole-run wipeout of permanent errors does not retry`() =
        runTest {
            val repository =
                FakePodcastRepository(
                    subscriptions = listOf(podcast("a"), podcast("b")),
                    failures = mapOf("a" to AppError.Http(404), "b" to AppError.Parsing()),
                )
            val summary = FeedRefresher(repository).refreshAll()

            assertEquals(
                FeedRefresher.Summary(total = 2, succeeded = 0, retryableFailures = 0, permanentFailures = 2),
                summary,
            )
            assertFalse(summary.shouldRetry)
        }

    @Test
    fun `a single permanent failure among transient ones blocks the retry`() =
        runTest {
            val repository =
                FakePodcastRepository(
                    subscriptions = listOf(podcast("a"), podcast("b")),
                    failures = mapOf("a" to AppError.Network(), "b" to AppError.NotFound()),
                )
            val summary = FeedRefresher(repository).refreshAll()

            assertEquals(
                FeedRefresher.Summary(total = 2, succeeded = 0, retryableFailures = 1, permanentFailures = 1),
                summary,
            )
            assertFalse(summary.shouldRetry)
        }

    @Test
    fun `an empty library is a no-op success`() =
        runTest {
            val summary = FeedRefresher(FakePodcastRepository()).refreshAll()

            assertEquals(
                FeedRefresher.Summary(total = 0, succeeded = 0, retryableFailures = 0, permanentFailures = 0),
                summary,
            )
            assertFalse(summary.shouldRetry)
        }

    private class FakePodcastRepository(
        private val subscriptions: List<Podcast> = emptyList(),
        private val failures: Map<String, AppError> = emptyMap(),
    ) : PodcastRepository {
        val refreshedIds = mutableSetOf<String>()

        override fun observeSubscriptions(): Flow<List<Podcast>> = MutableStateFlow(subscriptions)

        override fun observePodcast(podcastId: String): Flow<Podcast?> = flowOf(null)

        override fun observeEpisodes(podcastId: String): Flow<List<Episode>> = flowOf(emptyList())

        override fun observeEpisode(episodeId: String): Flow<Episode?> = flowOf(null)

        override fun observeRecentEpisodes(limit: Int): Flow<List<RecentEpisode>> = flowOf(emptyList())

        override suspend fun subscribe(feedUrl: String): Outcome<String> = Outcome.Success(feedUrl)

        override suspend fun refresh(podcastId: String): Outcome<Unit> {
            refreshedIds.add(podcastId)
            return failures[podcastId]?.let { Outcome.Failure(it) } ?: Outcome.Success(Unit)
        }

        override suspend fun refreshAll(): List<Outcome<Unit>> = subscriptions.map { refresh(it.id) }
    }
}
