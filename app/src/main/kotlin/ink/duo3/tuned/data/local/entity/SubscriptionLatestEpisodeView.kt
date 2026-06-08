package ink.duo3.tuned.data.local.entity

/**
 * Read-only projection joining each subscription's latest episode with the podcast's title and
 * artwork. Used by [ink.duo3.tuned.data.local.dao.EpisodeDao.observeLatestPerSubscription] for the
 * home screen's "Subscribed" row. Room maps the aliased columns automatically.
 */
data class SubscriptionLatestEpisodeView(
    val id: String,
    val podcastId: String,
    val title: String?,
    val description: String?,
    val enclosureUrl: String?,
    val artworkUrl: String?,
    val publishedAt: Long,
    val durationMs: Long?,
    val podcastTitle: String?,
    val podcastArtworkUrl: String?,
)
