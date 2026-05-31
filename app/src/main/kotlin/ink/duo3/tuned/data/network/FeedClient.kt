package ink.duo3.tuned.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import java.io.ByteArrayOutputStream
import java.io.IOException

/** Raised when a feed responds with a non-success, non-304 status. */
class FeedHttpException(
    val code: Int,
) : Exception("Feed request failed with HTTP $code")

/**
 * Raised when a feed body exceeds [FeedClient]'s size cap. Extends [IOException] so
 * the repository's transport-error mapping treats an oversized response like any
 * other read failure.
 */
class FeedTooLargeException(
    val maxBytes: Long,
) : IOException("Feed response exceeds the $maxBytes-byte limit")

/**
 * Fetches raw RSS bytes over HTTP with conditional-request support. Redirects are
 * followed by the underlying [HttpClient]; [Fetched.finalUrl] reports where the feed
 * actually resolved so callers can persist the post-redirect URL. The body is read
 * with a size cap so a hostile or misconfigured server cannot exhaust memory.
 */
class FeedClient(
    private val httpClient: HttpClient,
    private val maxFeedBytes: Long = MAX_FEED_BYTES,
) {
    sealed interface Result

    // Plain class (not data): holds a ByteArray, whose value-equality semantics we
    // don't need and which would otherwise trip detekt's ArrayInDataClass.
    class Fetched(
        val finalUrl: String,
        val body: ByteArray,
        val etag: String?,
        val lastModified: String?,
    ) : Result

    data object NotModified : Result

    suspend fun fetch(
        url: String,
        etag: String?,
        lastModified: String?,
    ): Result {
        val response =
            httpClient.get(url) {
                etag?.let { header(HttpHeaders.IfNoneMatch, it) }
                lastModified?.let { header(HttpHeaders.IfModifiedSince, it) }
            }
        val status = response.status
        if (status == HttpStatusCode.NotModified) return NotModified
        if (status.value !in SUCCESS_RANGE) throw FeedHttpException(status.value)
        return Fetched(
            finalUrl =
                response.call.request.url
                    .toString(),
            body = response.readBounded(maxFeedBytes),
            etag = response.headers[HttpHeaders.ETag],
            lastModified = response.headers[HttpHeaders.LastModified],
        )
    }

    // Rejects an oversized Content-Length up front, then streams the (possibly
    // chunked or decoded) body, aborting as soon as it crosses the cap.
    private suspend fun HttpResponse.readBounded(max: Long): ByteArray {
        contentLength()?.let { declared -> if (declared > max) throw FeedTooLargeException(max) }
        val channel = bodyAsChannel()
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(READ_CHUNK)
        var total = 0L
        while (true) {
            val read = channel.readAvailable(buffer, 0, buffer.size)
            if (read == -1) break
            total += read
            if (total > max) throw FeedTooLargeException(max)
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private companion object {
        val SUCCESS_RANGE = 200..299
        const val MAX_FEED_BYTES = 32L * 1024 * 1024
        const val READ_CHUNK = 8 * 1024
    }
}
