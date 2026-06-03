package ink.duo3.tuned.data.repository

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.data.network.ItunesSearchApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRepositoryImplTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun repo(engine: MockEngine) = SearchRepositoryImpl(ItunesSearchApi(HttpClient(engine), json))

    @Test
    fun `maps itunes results into domain models`() =
        runBlocking {
            val engine = MockEngine { respond(RESPONSE, HttpStatusCode.OK, JS_CONTENT_TYPE) }
            val result = repo(engine).searchPodcasts("design")

            val results = (result as Outcome.Success).value
            assertEquals(1, results.size)
            val first = results.single()
            assertEquals("https://feed.example/rss", first.feedUrl)
            assertEquals("Design Matters", first.title)
            assertEquals("Debbie Millman", first.author)
            assertEquals("https://img.example/600.jpg", first.artworkUrl)
            assertEquals(120, first.episodeCount)
        }

    @Test
    fun `falls back to the 100px artwork when 600 is absent`() =
        runBlocking {
            val engine = MockEngine { respond(SMALL_ART_RESPONSE, HttpStatusCode.OK, JS_CONTENT_TYPE) }
            val result = repo(engine).searchPodcasts("design")

            assertEquals("https://img.example/100.jpg", (result as Outcome.Success).value.single().artworkUrl)
        }

    @Test
    fun `drops entries without a feed url or title`() =
        runBlocking {
            val engine = MockEngine { respond(PARTIAL_RESPONSE, HttpStatusCode.OK, JS_CONTENT_TYPE) }
            val result = repo(engine).searchPodcasts("design")

            assertEquals(emptyList<String>(), (result as Outcome.Success).value.map { it.title })
        }

    @Test
    fun `deduplicates results that repeat a feed url`() =
        runBlocking {
            val engine = MockEngine { respond(DUPLICATE_RESPONSE, HttpStatusCode.OK, JS_CONTENT_TYPE) }
            val result = repo(engine).searchPodcasts("design")

            assertEquals(listOf("https://feed.example/rss"), (result as Outcome.Success).value.map { it.feedUrl })
        }

    @Test
    fun `an empty result set is a success with no results`() =
        runBlocking {
            val engine = MockEngine { respond(EMPTY_RESPONSE, HttpStatusCode.OK, JS_CONTENT_TYPE) }
            val result = repo(engine).searchPodcasts("zzz")

            assertTrue((result as Outcome.Success).value.isEmpty())
        }

    @Test
    fun `a server error maps to Failure Http with the status code`() =
        runBlocking {
            val engine = MockEngine { respond("oops", HttpStatusCode.InternalServerError) }
            val result = repo(engine).searchPodcasts("design")

            assertEquals(500, ((result as Outcome.Failure).error as AppError.Http).code)
        }

    @Test
    fun `a malformed body maps to Failure Parsing`() =
        runBlocking {
            val engine = MockEngine { respond("not json at all", HttpStatusCode.OK, JS_CONTENT_TYPE) }
            val result = repo(engine).searchPodcasts("design")

            assertTrue((result as Outcome.Failure).error is AppError.Parsing)
        }

    @Test
    fun `requests carry the podcast search parameters`() =
        runBlocking {
            var requestedUrl: String? = null
            val engine =
                MockEngine { request ->
                    requestedUrl = request.url.toString()
                    respond(EMPTY_RESPONSE, HttpStatusCode.OK, JS_CONTENT_TYPE)
                }
            repo(engine).searchPodcasts("the daily")

            val url = requireNotNull(requestedUrl) { "expected a request" }
            assertTrue(url, url.contains("media=podcast"))
            assertTrue(url, url.contains("entity=podcast"))
            assertTrue(url, url.contains("term=the+daily") || url.contains("term=the%20daily"))
        }

    private companion object {
        val JS_CONTENT_TYPE = headersOf(HttpHeaders.ContentType, "text/javascript; charset=utf-8")
        val RESPONSE =
            """
            {"resultCount":1,"results":[
              {"collectionName":"Design Matters","artistName":"Debbie Millman",
               "feedUrl":"https://feed.example/rss",
               "artworkUrl600":"https://img.example/600.jpg",
               "artworkUrl100":"https://img.example/100.jpg","trackCount":120,
               "primaryGenreName":"Arts"}
            ]}
            """.trimIndent()
        val SMALL_ART_RESPONSE =
            """
            {"resultCount":1,"results":[
              {"collectionName":"Design Matters","feedUrl":"https://feed.example/rss",
               "artworkUrl100":"https://img.example/100.jpg"}
            ]}
            """.trimIndent()
        val PARTIAL_RESPONSE =
            """
            {"resultCount":2,"results":[
              {"collectionName":"No Feed"},
              {"feedUrl":"https://feed.example/rss"}
            ]}
            """.trimIndent()
        val DUPLICATE_RESPONSE =
            """
            {"resultCount":2,"results":[
              {"collectionName":"Design Matters","feedUrl":"https://feed.example/rss"},
              {"collectionName":"Design Matters (mirror)","feedUrl":"https://feed.example/rss"}
            ]}
            """.trimIndent()
        const val EMPTY_RESPONSE = """{"resultCount":0,"results":[]}"""
    }
}
