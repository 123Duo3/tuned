package ink.duo3.tuned.data.network

import ink.duo3.tuned.data.model.ItunesChartsResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

/**
 * Thin client over Apple's "top podcasts" charts endpoint
 * (`/{country}/rss/toppodcasts/limit={n}[/genre={id}]/json`). It returns only collection ids
 * in chart-rank order; feed URLs are resolved separately via [ItunesSearchApi.lookup]. A non-2xx
 * status raises [FeedHttpException]; a malformed body surfaces as a
 * [kotlinx.serialization.SerializationException] — both mapped to typed errors upstream.
 */
class ItunesChartsApi(
    private val httpClient: HttpClient,
    private val json: Json,
    private val baseUrl: String = ITUNES_RSS_URL,
) {
    /** Top podcast collection ids for [country], optionally narrowed to an iTunes [genreId]. */
    suspend fun topPodcastIds(
        country: String,
        limit: Int = DEFAULT_LIMIT,
        genreId: Int? = null,
    ): List<String> {
        val url =
            buildString {
                append(baseUrl)
                append('/')
                append(country.lowercase())
                append("/rss/toppodcasts/limit=")
                append(limit)
                if (genreId != null) {
                    append("/genre=")
                    append(genreId)
                }
                append("/json")
            }
        val response = httpClient.get(url)
        if (response.status.value !in SUCCESS_RANGE) throw FeedHttpException(response.status.value)
        return json
            .decodeFromString<ItunesChartsResponse>(response.bodyAsText())
            .feed
            .entry
            .mapNotNull { entry ->
                val id = entry.id.attributes.imId
                id?.takeIf(String::isNotBlank)
            }
    }

    private companion object {
        const val ITUNES_RSS_URL = "https://itunes.apple.com"
        const val DEFAULT_LIMIT = 25
        val SUCCESS_RANGE = 200..299
    }
}
