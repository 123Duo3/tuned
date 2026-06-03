package ink.duo3.tuned.domain.model

/**
 * A podcast surfaced by a search source, carrying just enough to render a result row and
 * subscribe. [feedUrl] is the RSS address handed to the subscribe pipeline, so a result
 * without one is dropped by the repository before it reaches the UI.
 */
data class PodcastSearchResult(
    val feedUrl: String,
    val title: String,
    val author: String?,
    val artworkUrl: String?,
    val episodeCount: Int?,
)
