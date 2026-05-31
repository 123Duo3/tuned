package ink.duo3.tuned.data.repository

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.data.local.FeedIdentity
import ink.duo3.tuned.data.local.TransactionRunner
import ink.duo3.tuned.data.local.dao.EpisodeDao
import ink.duo3.tuned.data.local.dao.PodcastDao
import ink.duo3.tuned.data.local.entity.EpisodeEntity
import ink.duo3.tuned.data.local.entity.PodcastEntity
import ink.duo3.tuned.data.network.FeedClient
import ink.duo3.tuned.data.network.RssFeedParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRedirect
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastRepositoryImplTest {
    private val podcastDao = FakePodcastDao()
    private val episodeDao = FakeEpisodeDao()

    private fun repo(
        engine: MockEngine,
        followRedirects: Boolean = false,
        now: () -> Long = { FIXED_NOW },
    ): PodcastRepositoryImpl {
        val client = if (followRedirects) HttpClient(engine) { install(HttpRedirect) } else HttpClient(engine)
        return PodcastRepositoryImpl(
            FeedClient(client),
            RssFeedParser(),
            podcastDao,
            episodeDao,
            DirectTransactionRunner,
            now,
        )
    }

    @Test
    fun `subscribe stores the podcast and its episodes`() =
        runBlocking {
            val engine = MockEngine { respond(RSS, HttpStatusCode.OK, headersOf(HttpHeaders.ETag, "\"v1\"")) }
            val result = repo(engine).subscribe(FEED_URL)

            val id = (result as Outcome.Success).value
            assertEquals(FeedIdentity.podcastId(FEED_URL), id)
            val stored = podcastDao.stored.getValue(id)
            assertEquals(FEED_URL, stored.canonicalFeedUrl)
            assertEquals(FEED_URL, stored.currentFeedUrl)
            assertEquals("\"v1\"", stored.etag)
            assertEquals(FIXED_NOW, stored.lastFetchedAt)
            assertEquals(1, episodeDao.stored.count { it.podcastId == id })
        }

    @Test
    fun `subscribe follows a redirect and records the resolved url`() =
        runBlocking {
            val engine =
                MockEngine { request ->
                    if (request.url.toString() == "https://old.example.com/feed") {
                        respond(
                            "",
                            HttpStatusCode.MovedPermanently,
                            headersOf(HttpHeaders.Location, "https://new.example.com/feed"),
                        )
                    } else {
                        respond(RSS, HttpStatusCode.OK)
                    }
                }
            val result = repo(engine, followRedirects = true).subscribe("https://old.example.com/feed")

            val id = (result as Outcome.Success).value
            assertEquals(FeedIdentity.podcastId("https://new.example.com/feed"), id)
            val stored = podcastDao.stored.getValue(id)
            assertEquals("https://new.example.com/feed", stored.canonicalFeedUrl)
            assertEquals("https://new.example.com/feed", stored.currentFeedUrl)
        }

    @Test
    fun `refresh updates currentFeedUrl on redirect but keeps the podcast id`() =
        runBlocking {
            repo(MockEngine { respond(RSS, HttpStatusCode.OK) }).subscribe(FEED_URL)
            val id = FeedIdentity.podcastId(FEED_URL)

            val redirecting =
                MockEngine { request ->
                    if (request.url.toString() == FEED_URL) {
                        respond("", HttpStatusCode.MovedPermanently, headersOf(HttpHeaders.Location, MOVED_URL))
                    } else {
                        respond(RSS, HttpStatusCode.OK)
                    }
                }
            val result = repo(redirecting, followRedirects = true).refresh(id)

            assertTrue(result is Outcome.Success)
            val stored = podcastDao.stored.getValue(id)
            assertEquals(FEED_URL, stored.canonicalFeedUrl)
            assertEquals(MOVED_URL, stored.currentFeedUrl)
        }

    @Test
    fun `refresh on 304 touches lastFetchedAt but writes nothing else`() =
        runBlocking {
            repo(MockEngine { respond(RSS, HttpStatusCode.OK, headersOf(HttpHeaders.ETag, "\"v1\"")) })
                .subscribe(FEED_URL)
            val id = FeedIdentity.podcastId(FEED_URL)
            val before = podcastDao.stored.getValue(id)
            val episodesBefore = episodeDao.stored.size

            val result =
                repo(MockEngine { respond("", HttpStatusCode.NotModified) }, now = { LATER_NOW }).refresh(id)

            assertTrue(result is Outcome.Success)
            val after = podcastDao.stored.getValue(id)
            assertEquals(LATER_NOW, after.lastFetchedAt)
            assertEquals(before.copy(lastFetchedAt = LATER_NOW), after)
            assertEquals(episodesBefore, episodeDao.stored.size)
        }

    @Test
    fun `subscribe maps an http error to Failure Http`() =
        runBlocking {
            val engine = MockEngine { respond("nope", HttpStatusCode.NotFound) }
            val result = repo(engine).subscribe(FEED_URL)

            val error = (result as Outcome.Failure).error
            assertEquals(404, (error as AppError.Http).code)
            assertTrue(podcastDao.stored.isEmpty())
        }

    @Test
    fun `subscribe maps a malformed feed to Failure Parsing`() =
        runBlocking {
            val engine = MockEngine { respond("<html><body>not rss</body></html>", HttpStatusCode.OK) }
            val result = repo(engine).subscribe(FEED_URL)

            assertTrue((result as Outcome.Failure).error is AppError.Parsing)
            assertTrue(podcastDao.stored.isEmpty())
        }

    @Test
    fun `refresh of an unknown podcast is NotFound`() =
        runBlocking {
            val engine = MockEngine { respond(RSS, HttpStatusCode.OK) }
            val result = repo(engine).refresh("missing")

            assertTrue((result as Outcome.Failure).error is AppError.NotFound)
        }

    private companion object {
        const val FEED_URL = "https://example.com/feed.xml"
        const val MOVED_URL = "https://cdn.example.com/feed.xml"
        const val FIXED_NOW = 1_000L
        const val LATER_NOW = 5_000L
        val RSS =
            """
            <rss version="2.0"><channel><title>Pod</title>
            <item><guid>g1</guid><title>E1</title>
            <enclosure url="https://cdn.example.com/1.mp3" type="audio/mpeg"/></item>
            </channel></rss>
            """.trimIndent()
    }
}

private object DirectTransactionRunner : TransactionRunner {
    override suspend fun <R> invoke(block: suspend () -> R): R = block()
}

private class FakePodcastDao : PodcastDao {
    val stored = linkedMapOf<String, PodcastEntity>()

    override suspend fun upsert(podcast: PodcastEntity) {
        stored[podcast.id] = podcast
    }

    override fun observeAll(): Flow<List<PodcastEntity>> = flowOf(stored.values.toList())

    override suspend fun findById(id: String): PodcastEntity? = stored[id]

    override suspend fun deleteById(id: String) {
        stored.remove(id)
    }
}

private class FakeEpisodeDao : EpisodeDao {
    val stored = mutableListOf<EpisodeEntity>()

    override suspend fun upsertAll(episodes: List<EpisodeEntity>) {
        for (episode in episodes) {
            stored.removeAll { it.id == episode.id }
            stored.add(episode)
        }
    }

    override fun observeByPodcast(podcastId: String) = flowOf(stored.filter { it.podcastId == podcastId })

    override suspend fun findById(id: String): EpisodeEntity? = stored.firstOrNull { it.id == id }
}
