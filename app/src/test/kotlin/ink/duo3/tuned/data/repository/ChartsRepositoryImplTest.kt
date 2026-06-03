package ink.duo3.tuned.data.repository

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.data.network.ItunesChartsApi
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

class ChartsRepositoryImplTest {
    private val json = Json { ignoreUnknownKeys = true }

    /** Routes charts vs lookup by path so a single engine serves the two-step flow. */
    private fun repo(
        chartsBody: String,
        chartsStatus: HttpStatusCode = HttpStatusCode.OK,
        lookupBody: String = EMPTY_LOOKUP,
        lookupStatus: HttpStatusCode = HttpStatusCode.OK,
        onLookup: (String) -> Unit = {},
    ): ChartsRepositoryImpl {
        val engine =
            MockEngine { request ->
                val url = request.url.toString()
                if (url.contains("/lookup")) {
                    onLookup(url)
                    respond(lookupBody, lookupStatus, JS_CONTENT_TYPE)
                } else {
                    respond(chartsBody, chartsStatus, JS_CONTENT_TYPE)
                }
            }
        val client = HttpClient(engine)
        return ChartsRepositoryImpl(ItunesChartsApi(client, json), ItunesSearchApi(client, json))
    }

    @Test
    fun `resolves chart ids into domain models preserving rank order`() =
        runBlocking {
            val result = repo(chartsBody = CHARTS_TWO, lookupBody = LOOKUP_TWO).topPodcasts("us")

            val titles = (result as Outcome.Success).value.map { it.title }
            assertEquals(listOf("First", "Second"), titles)
        }

    @Test
    fun `re-orders lookup results that arrive out of chart order`() =
        runBlocking {
            val result = repo(chartsBody = CHARTS_TWO, lookupBody = LOOKUP_TWO_REVERSED).topPodcasts("us")

            assertEquals(listOf("First", "Second"), (result as Outcome.Success).value.map { it.title })
        }

    @Test
    fun `drops chart entries the lookup can't resolve to a feed url`() =
        runBlocking {
            val result = repo(chartsBody = CHARTS_TWO, lookupBody = LOOKUP_ONE_NO_FEED).topPodcasts("us")

            assertEquals(listOf("First"), (result as Outcome.Success).value.map { it.title })
        }

    @Test
    fun `an empty chart is a success with no lookup call`() =
        runBlocking {
            var lookedUp = false
            val result =
                repo(chartsBody = EMPTY_CHARTS, onLookup = { lookedUp = true }).topPodcasts("us")

            assertTrue((result as Outcome.Success).value.isEmpty())
            assertTrue("no lookup expected for an empty chart", !lookedUp)
        }

    @Test
    fun `a charts server error maps to Failure Http with the status code`() =
        runBlocking {
            val result =
                repo(chartsBody = "oops", chartsStatus = HttpStatusCode.InternalServerError).topPodcasts("us")

            assertEquals(500, ((result as Outcome.Failure).error as AppError.Http).code)
        }

    @Test
    fun `a malformed charts body maps to Failure Parsing`() =
        runBlocking {
            val result = repo(chartsBody = "not json").topPodcasts("us")

            assertTrue((result as Outcome.Failure).error is AppError.Parsing)
        }

    @Test
    fun `the country and optional genre shape the charts request url`() =
        runBlocking {
            var chartsUrl: String? = null
            val engine =
                MockEngine { request ->
                    val url = request.url.toString()
                    if (!url.contains("/lookup")) chartsUrl = url
                    val body =
                        if (url.contains("/lookup")) EMPTY_LOOKUP else EMPTY_CHARTS
                    respond(body, HttpStatusCode.OK, JS_CONTENT_TYPE)
                }
            val client = HttpClient(engine)
            ChartsRepositoryImpl(ItunesChartsApi(client, json), ItunesSearchApi(client, json))
                .topPodcasts("DE", genreId = 1310)

            val url = requireNotNull(chartsUrl) { "expected a charts request" }
            assertTrue(url, url.contains("/de/rss/toppodcasts"))
            assertTrue(url, url.contains("genre=1310"))
        }

    private companion object {
        val JS_CONTENT_TYPE = headersOf(HttpHeaders.ContentType, "text/javascript; charset=utf-8")

        val CHARTS_TWO =
            """
            {"feed":{"entry":[
              {"id":{"attributes":{"im:id":"111"}}},
              {"id":{"attributes":{"im:id":"222"}}}
            ]}}
            """.trimIndent()
        const val EMPTY_CHARTS = """{"feed":{"entry":[]}}"""
        const val EMPTY_LOOKUP = """{"resultCount":0,"results":[]}"""

        val LOOKUP_TWO =
            """
            {"resultCount":2,"results":[
              {"collectionId":111,"collectionName":"First","feedUrl":"https://feed.example/1"},
              {"collectionId":222,"collectionName":"Second","feedUrl":"https://feed.example/2"}
            ]}
            """.trimIndent()
        val LOOKUP_TWO_REVERSED =
            """
            {"resultCount":2,"results":[
              {"collectionId":222,"collectionName":"Second","feedUrl":"https://feed.example/2"},
              {"collectionId":111,"collectionName":"First","feedUrl":"https://feed.example/1"}
            ]}
            """.trimIndent()
        val LOOKUP_ONE_NO_FEED =
            """
            {"resultCount":2,"results":[
              {"collectionId":111,"collectionName":"First","feedUrl":"https://feed.example/1"},
              {"collectionId":222,"collectionName":"Second"}
            ]}
            """.trimIndent()
    }
}
