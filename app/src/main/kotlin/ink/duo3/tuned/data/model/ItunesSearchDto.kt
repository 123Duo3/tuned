package ink.duo3.tuned.data.model

import kotlinx.serialization.Serializable

/**
 * Wire shape of the iTunes Search API podcast response. Only the fields the app uses are
 * declared; the parser ignores the rest (see the [kotlinx.serialization.json.Json] instance
 * configured with `ignoreUnknownKeys`). The endpoint often replies with `text/javascript`,
 * so the body is decoded explicitly rather than via content negotiation.
 */
@Serializable
data class ItunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ItunesPodcastDto> = emptyList(),
)

@Serializable
data class ItunesPodcastDto(
    val collectionId: Long? = null,
    val collectionName: String? = null,
    val artistName: String? = null,
    val feedUrl: String? = null,
    val artworkUrl600: String? = null,
    val artworkUrl100: String? = null,
    val trackCount: Int? = null,
    val primaryGenreName: String? = null,
)
