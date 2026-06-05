package ink.duo3.tuned.data.network

import ink.duo3.tuned.data.model.ChaptersDocumentDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

/**
 * Thin client that fetches a Podcasting 2.0 chapters JSON document. A non-2xx status
 * raises [FeedHttpException]; a malformed body surfaces as a
 * [kotlinx.serialization.SerializationException] — both mapped to typed errors upstream.
 * The body is decoded explicitly because chapter hosts often serve the file as
 * `application/octet-stream` or `text/plain` rather than `application/json`.
 */
class ChaptersApi(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    suspend fun fetch(url: String): ChaptersDocumentDto {
        val response = httpClient.get(url)
        if (response.status.value !in SUCCESS_RANGE) throw FeedHttpException(response.status.value)
        return json.decodeFromString(response.bodyAsText())
    }

    private companion object {
        val SUCCESS_RANGE = 200..299
    }
}
