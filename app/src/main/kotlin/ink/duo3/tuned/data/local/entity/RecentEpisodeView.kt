package ink.duo3.tuned.data.local.entity

/**
 * Read-only projection joining an episode with its parent podcast's title and artwork.
 * Used by [ink.duo3.tuned.data.local.dao.EpisodeDao.observeRecent] for the home screen's
 * "Recently Updated" card. Room maps the aliased columns automatically.
 */
data class RecentEpisodeView(
    val id: String,
    val podcastId: String,
    val title: String?,
    val description: String?,
    val artworkUrl: String?,
    val publishedAt: Long,
    val durationMs: Long?,
    val podcastTitle: String?,
    val podcastArtworkUrl: String?,
)
