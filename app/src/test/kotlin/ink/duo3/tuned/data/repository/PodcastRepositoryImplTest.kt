package ink.duo3.tuned.data.repository

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.data.local.FeedIdentity
import ink.duo3.tuned.data.local.TransactionRunner
import ink.duo3.tuned.data.local.dao.EpisodeDao
import ink.duo3.tuned.data.local.dao.PodcastDao
import ink.duo3.tuned.data.local.entity.EpisodeEntity
import ink.duo3.tuned.data.local.entity.PodcastEntity
import ink.duo3.tuned.data.local.entity.RecentEpisodeView
import ink.duo3.tuned.data.local.entity.SubscriptionLatestEpisodeView
import ink.duo3.tuned.data.network.FeedClient
import ink.duo3.tuned.data.network.FeedResolver
import ink.duo3.tuned.data.network.RssFeedParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRedirect
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
            FeedResolver(FeedClient(client), RssFeedParser()),
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
            assertEquals("Pod", stored.title)
            assertEquals("Pod Author", stored.author)
            assertEquals("A pod.", stored.description)
            assertEquals("https://example.com/art.jpg", stored.artworkUrl)
            assertEquals(1, episodeDao.stored.count { it.podcastId == id })
            val episode = episodeDao.stored.single { it.podcastId == id }
            assertEquals("E1", episode.title)
            assertEquals("Notes 1", episode.description)
        }

    @Test
    fun `subscribe adds https scheme when omitted`() =
        runBlocking {
            var requestedUrl: String? = null
            val engine =
                MockEngine { request ->
                    requestedUrl = request.url.toString()
                    respond(RSS, HttpStatusCode.OK)
                }
            val result = repo(engine).subscribe("  example.com/feed.xml  ")

            val id = (result as Outcome.Success).value
            assertEquals(FEED_URL, requestedUrl)
            assertEquals(FeedIdentity.podcastId(FEED_URL), id)
            assertEquals(FEED_URL, podcastDao.stored.getValue(id).canonicalFeedUrl)
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
    fun `refresh follows a new-feed-url announced after subscribe and updates only currentFeedUrl`() =
        runBlocking {
            repo(MockEngine { respond(RSS, HttpStatusCode.OK) }).subscribe(FEED_URL)
            val id = FeedIdentity.podcastId(FEED_URL)

            val migrating =
                MockEngine { request ->
                    if (request.url.toString() == FEED_URL) {
                        respond(MIGRATING_RSS, HttpStatusCode.OK)
                    } else {
                        respond(RSS, HttpStatusCode.OK)
                    }
                }
            val result = repo(migrating).refresh(id)

            assertTrue(result is Outcome.Success)
            val stored = podcastDao.stored.getValue(id)
            assertEquals(id, FeedIdentity.podcastId(stored.canonicalFeedUrl))
            assertEquals(FEED_URL, stored.canonicalFeedUrl)
            assertEquals(MOVED_URL, stored.currentFeedUrl)
        }

    @Test
    fun `subscribe follows itunes new-feed-url to the canonical feed`() =
        runBlocking {
            val canonical = "https://example.com/canonical.xml"
            val engine =
                MockEngine { request ->
                    if (request.url.toString() == FEED_URL) {
                        respond(MOVED_RSS, HttpStatusCode.OK)
                    } else {
                        respond(RSS, HttpStatusCode.OK)
                    }
                }
            val result = repo(engine).subscribe(FEED_URL)

            val id = (result as Outcome.Success).value
            assertEquals(FeedIdentity.podcastId(canonical), id)
            val stored = podcastDao.stored.getValue(id)
            assertEquals(canonical, stored.canonicalFeedUrl)
            assertEquals(canonical, stored.currentFeedUrl)
            assertEquals("Pod", stored.title)
            assertEquals(1, episodeDao.stored.count { it.podcastId == id })
        }

    @Test
    fun `subscribe stops following a self-referential new-feed-url`() =
        runBlocking {
            val engine = MockEngine { respond(SELF_MOVED_RSS, HttpStatusCode.OK) }
            val result = repo(engine).subscribe(FEED_URL)

            val id = (result as Outcome.Success).value
            assertEquals(FeedIdentity.podcastId(FEED_URL), id)
            assertEquals(FEED_URL, podcastDao.stored.getValue(id).canonicalFeedUrl)
        }

    @Test
    fun `subscribe terminates when a new-feed-url http-redirects back to the origin`() =
        runBlocking {
            // FEED_URL declares new-feed-url B; requesting B 301s back to FEED_URL. The
            // resolved url never changes, so only the requested-url set + hop counter
            // can stop the walk.
            val engine =
                MockEngine { request ->
                    if (request.url.toString() == REDIRECT_B) {
                        respond("", HttpStatusCode.MovedPermanently, headersOf(HttpHeaders.Location, FEED_URL))
                    } else {
                        respond(NEW_FEED_TO_B_RSS, HttpStatusCode.OK)
                    }
                }
            val result = repo(engine, followRedirects = true).subscribe(FEED_URL)

            val id = (result as Outcome.Success).value
            assertEquals(FeedIdentity.podcastId(FEED_URL), id)
        }

    @Test
    fun `subscribe discovers the feed from an html autodiscovery link`() =
        runBlocking {
            val discovered = "https://example.com/discovered.xml"
            val engine =
                MockEngine { request ->
                    if (request.url.toString() == discovered) {
                        respond(RSS, HttpStatusCode.OK)
                    } else {
                        respond(HTML_WITH_LINK, HttpStatusCode.OK)
                    }
                }
            val result = repo(engine).subscribe(FEED_URL)

            val id = (result as Outcome.Success).value
            assertEquals(FeedIdentity.podcastId(discovered), id)
            assertEquals(discovered, podcastDao.stored.getValue(id).canonicalFeedUrl)
            assertEquals(1, episodeDao.stored.count { it.podcastId == id })
        }

    @Test
    fun `subscribe falls back to guessing a conventional feed path`() =
        runBlocking {
            val guessed = "https://example.com/feed"
            val engine =
                MockEngine { request ->
                    if (request.url.toString() == guessed) {
                        respond(RSS, HttpStatusCode.OK)
                    } else {
                        respond(HTML_NO_LINK, HttpStatusCode.OK)
                    }
                }
            val result = repo(engine).subscribe("https://example.com")

            val id = (result as Outcome.Success).value
            assertEquals(FeedIdentity.podcastId(guessed), id)
            assertEquals(guessed, podcastDao.stored.getValue(id).canonicalFeedUrl)
        }

    @Test
    fun `subscribe surfaces Parsing when discovery finds no feed`() =
        runBlocking {
            val engine = MockEngine { respond(HTML_NO_LINK, HttpStatusCode.OK) }
            val result = repo(engine).subscribe("https://example.com")

            assertTrue((result as Outcome.Failure).error is AppError.Parsing)
            assertTrue(podcastDao.stored.isEmpty())
        }

    @Test
    fun `discovery guesses feed paths under the redirected final url`() =
        runBlocking {
            // The typed host 301s to the real domain; the feed only exists there, so the
            // path guesses must be built from the resolved url, not the typed one.
            val canonical = "https://www.example.com/"
            val canonicalFeed = "https://www.example.com/feed"
            val engine =
                MockEngine { request ->
                    when (request.url.toString()) {
                        "https://example.com" ->
                            respond("", HttpStatusCode.MovedPermanently, headersOf(HttpHeaders.Location, canonical))
                        canonicalFeed -> respond(RSS, HttpStatusCode.OK)
                        else -> respond(HTML_NO_LINK, HttpStatusCode.OK)
                    }
                }
            val result = repo(engine, followRedirects = true).subscribe("https://example.com")

            val id = (result as Outcome.Success).value
            assertEquals(FeedIdentity.podcastId(canonicalFeed), id)
            assertEquals(canonicalFeed, podcastDao.stored.getValue(id).canonicalFeedUrl)
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
    fun `observeSubscriptions maps stored podcasts to domain models`() =
        runBlocking {
            repo(MockEngine { respond(RSS, HttpStatusCode.OK) }).subscribe(FEED_URL)
            val id = FeedIdentity.podcastId(FEED_URL)

            val podcast = repo(MockEngine { respond(RSS, HttpStatusCode.OK) }).observeSubscriptions().first().single()

            assertEquals(id, podcast.id)
            assertEquals(FEED_URL, podcast.feedUrl)
            assertEquals("Pod", podcast.title)
            assertEquals("Pod Author", podcast.author)
            assertEquals("https://example.com/art.jpg", podcast.artworkUrl)
        }

    @Test
    fun `observeEpisodes maps a podcast's episodes to domain models`() =
        runBlocking {
            repo(MockEngine { respond(RSS, HttpStatusCode.OK) }).subscribe(FEED_URL)
            val id = FeedIdentity.podcastId(FEED_URL)

            val episode =
                repo(MockEngine { respond(RSS, HttpStatusCode.OK) }).observeEpisodes(id).first().single()

            assertEquals(id, episode.podcastId)
            assertEquals("E1", episode.title)
            assertEquals("Notes 1", episode.description)
            assertEquals("https://cdn.example.com/1.mp3", episode.enclosureUrl)
        }

    @Test
    fun `subscribe to a non-http url fails with InvalidUrl and never hits the network`() =
        runBlocking {
            var requested = false
            val engine =
                MockEngine {
                    requested = true
                    respond(RSS, HttpStatusCode.OK)
                }
            val result = repo(engine).subscribe("not a url")

            assertTrue((result as Outcome.Failure).error is AppError.InvalidUrl)
            assertTrue(podcastDao.stored.isEmpty())
            assertEquals(false, requested)
        }

    @Test
    fun `subscribe to an explicit non-http scheme fails with InvalidUrl`() =
        runBlocking {
            val engine = MockEngine { error("network must not be called") }
            val result = repo(engine).subscribe("ftp://example.com/feed.xml")

            assertTrue((result as Outcome.Failure).error is AppError.InvalidUrl)
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
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"><channel>
            <title>Pod</title><description>A pod.</description>
            <itunes:author>Pod Author</itunes:author>
            <itunes:image href="https://example.com/art.jpg"/>
            <item><guid>g1</guid><title>E1</title><description>Notes 1</description>
            <enclosure url="https://cdn.example.com/1.mp3" type="audio/mpeg"/></item>
            </channel></rss>
            """.trimIndent()
        val MOVED_RSS =
            """
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"><channel>
            <title>Old Mirror</title><description>Stale.</description>
            <itunes:new-feed-url>https://example.com/canonical.xml</itunes:new-feed-url>
            </channel></rss>
            """.trimIndent()
        val SELF_MOVED_RSS =
            """
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"><channel>
            <title>Pod</title><description>A pod.</description>
            <itunes:new-feed-url>$FEED_URL</itunes:new-feed-url>
            <item><guid>g1</guid><title>E1</title>
            <enclosure url="https://cdn.example.com/1.mp3" type="audio/mpeg"/></item>
            </channel></rss>
            """.trimIndent()
        val HTML_WITH_LINK =
            """
            <html><head>
            <link rel="alternate" type="application/rss+xml" href="https://example.com/discovered.xml">
            </head><body>a site</body></html>
            """.trimIndent()
        const val HTML_NO_LINK = "<html><head><title>A site</title></head><body>no feed here</body></html>"
        const val REDIRECT_B = "https://example.com/moved-b.xml"
        val NEW_FEED_TO_B_RSS =
            """
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"><channel>
            <title>Pod</title><description>A pod.</description>
            <itunes:new-feed-url>$REDIRECT_B</itunes:new-feed-url>
            <item><guid>g1</guid><title>E1</title>
            <enclosure url="https://cdn.example.com/1.mp3" type="audio/mpeg"/></item>
            </channel></rss>
            """.trimIndent()
        val MIGRATING_RSS =
            """
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"><channel>
            <title>Pod</title><description>A pod.</description>
            <itunes:new-feed-url>$MOVED_URL</itunes:new-feed-url>
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

    override fun observeById(id: String): Flow<PodcastEntity?> = flowOf(stored[id])

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

    override fun observeById(id: String) = flowOf(stored.firstOrNull { it.id == id })

    override suspend fun findById(id: String): EpisodeEntity? = stored.firstOrNull { it.id == id }

    override fun observeRecent(limit: Int): Flow<List<RecentEpisodeView>> = flowOf(emptyList())

    override fun observeLatestPerSubscription(): Flow<List<SubscriptionLatestEpisodeView>> = flowOf(emptyList())
}
