package ink.duo3.tuned.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.request.HttpRequestData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FeedClientTest {
    @Test
    fun `fetch returns body and conditional-response headers`() =
        runBlocking {
            val headers =
                Headers.build {
                    append(HttpHeaders.ETag, "\"v1\"")
                    append(HttpHeaders.LastModified, "Wed, 21 Oct 2025 07:28:00 GMT")
                }
            val engine = MockEngine { respond("<rss/>", HttpStatusCode.OK, headers) }
            val result = FeedClient(HttpClient(engine)).fetch("https://example.com/feed.xml", null, null)

            val fetched = result as FeedClient.Fetched
            assertEquals("<rss/>", String(fetched.body))
            assertEquals("\"v1\"", fetched.etag)
            assertEquals("Wed, 21 Oct 2025 07:28:00 GMT", fetched.lastModified)
        }

    @Test
    fun `fetch sends conditional request headers when validators are provided`() =
        runBlocking {
            var captured: HttpRequestData? = null
            val engine =
                MockEngine { request ->
                    captured = request
                    respond("<rss/>", HttpStatusCode.OK)
                }
            FeedClient(HttpClient(engine))
                .fetch("https://example.com/feed.xml", "\"v1\"", "Wed, 21 Oct 2025 07:28:00 GMT")

            assertEquals("\"v1\"", captured?.headers?.get(HttpHeaders.IfNoneMatch))
            assertEquals("Wed, 21 Oct 2025 07:28:00 GMT", captured?.headers?.get(HttpHeaders.IfModifiedSince))
        }

    @Test
    fun `304 maps to NotModified`() =
        runBlocking {
            val engine = MockEngine { respond("", HttpStatusCode.NotModified) }
            val result = FeedClient(HttpClient(engine)).fetch("https://example.com/feed.xml", "\"v1\"", null)

            assertEquals(FeedClient.NotModified, result)
        }

    @Test
    fun `redirect is followed and final url is reported`() =
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
                        respond("<rss/>", HttpStatusCode.OK)
                    }
                }
            val client = FeedClient(HttpClient(engine) { install(HttpRedirect) })
            val result = client.fetch("https://old.example.com/feed", null, null)

            assertEquals("https://new.example.com/feed", (result as FeedClient.Fetched).finalUrl)
        }

    @Test
    fun `a body over the cap throws FeedTooLargeException`() {
        val engine = MockEngine { respond("x".repeat(100), HttpStatusCode.OK) }
        assertThrows(FeedTooLargeException::class.java) {
            runBlocking {
                FeedClient(HttpClient(engine), maxFeedBytes = 8)
                    .fetch("https://example.com/feed.xml", null, null)
            }
        }
    }

    @Test
    fun `non-success status throws FeedHttpException with the code`() {
        val engine = MockEngine { respond("nope", HttpStatusCode.NotFound) }
        val exception =
            assertThrows(FeedHttpException::class.java) {
                runBlocking { FeedClient(HttpClient(engine)).fetch("https://example.com/feed.xml", null, null) }
            }

        assertEquals(404, exception.code)
    }
}
