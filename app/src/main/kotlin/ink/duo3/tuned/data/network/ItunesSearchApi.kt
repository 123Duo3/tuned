package ink.duo3.tuned.data.network

import ink.duo3.tuned.data.model.ItunesSearchResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

/**
 * Thin client over the iTunes Search API podcast endpoint. The endpoint is free and
 * needs no auth, but is rate-limited — callers debounce. A non-2xx status raises
 * [FeedHttpException] (shared with the feed pipeline); a malformed body surfaces as a
 * [kotlinx.serialization.SerializationException], both mapped to typed errors upstream.
 */
class ItunesSearchApi(
    private val httpClient: HttpClient,
    private val json: Json,
    private val baseUrl: String = ITUNES_SEARCH_URL,
) {
    suspend fun search(
        term: String,
        limit: Int = DEFAULT_LIMIT,
        country: String? = null,
    ): ItunesSearchResponse {
        val response =
            httpClient.get(baseUrl) {
                parameter("media", "podcast")
                parameter("entity", "podcast")
                parameter("term", term)
                parameter("limit", limit)
                country?.let { parameter("country", it) }
            }
        if (response.status.value !in SUCCESS_RANGE) throw FeedHttpException(response.status.value)
        return json.decodeFromString(response.bodyAsText())
    }

    private companion object {
        const val ITUNES_SEARCH_URL = "https://itunes.apple.com/search"
        const val DEFAULT_LIMIT = 25
        val SUCCESS_RANGE = 200..299
    }
}
