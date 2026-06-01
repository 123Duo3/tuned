package ink.duo3.tuned.data.model

/**
 * Raw extraction from an RSS document — no identity, dedup, or redirect handling.
 * Those belong to the import mapper that turns this into Room entities.
 */
data class ParsedFeed(
    val title: String?,
    val link: String?,
    val description: String?,
    val author: String?,
    val artworkUrl: String?,
    val newFeedUrl: String?,
    val items: List<ParsedEpisode>,
)

data class ParsedEpisode(
    val guid: String?,
    val title: String?,
    val description: String?,
    val enclosureUrl: String?,
    val publishedAtMs: Long?,
    val durationMs: Long?,
)
