package ink.duo3.tuned.domain.model

/**
 * A subscribed podcast paired with its most recent episode — the unit of the home
 * "Subscribed" row, ordered by [publishedAtMs] so the most recently updated subscriptions
 * come first. [description] is the episode's raw HTML show notes (shown as plain-text filler
 * under the title); [artworkUrl] is the episode's own art, with [podcastArtworkUrl] as the
 * fallback. [enclosureUrl] is null for text-only items, which are then not playable.
 */
data class SubscriptionEpisode(
    val podcastId: String,
    val podcastTitle: String?,
    val podcastArtworkUrl: String?,
    val episodeId: String,
    val title: String?,
    val description: String?,
    val artworkUrl: String?,
    val enclosureUrl: String?,
    val publishedAtMs: Long,
    val durationMs: Long?,
)
