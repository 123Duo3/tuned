package ink.duo3.tuned.domain.model

/**
 * An episode enriched with its parent podcast's display fields, used by the home
 * screen's "Recently Updated" card. Callers use [podcastArtworkUrl] as fallback when
 * [artworkUrl] is null.
 */
data class RecentEpisode(
    val id: String,
    val podcastId: String,
    val title: String?,
    val description: String?,
    val artworkUrl: String?,
    val publishedAtMs: Long,
    val durationMs: Long?,
    val podcastTitle: String?,
    val podcastArtworkUrl: String?,
)
