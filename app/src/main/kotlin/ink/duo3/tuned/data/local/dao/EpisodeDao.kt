package ink.duo3.tuned.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ink.duo3.tuned.data.local.entity.EpisodeEntity
import ink.duo3.tuned.data.local.entity.RecentEpisodeView
import ink.duo3.tuned.data.local.entity.SubscriptionLatestEpisodeView
import kotlinx.coroutines.flow.Flow

internal const val RECENT_EPISODES_QUERY =
    """
    SELECT e.id, e.podcastId, e.title, e.artworkUrl, e.publishedAt, e.durationMs,
        p.title AS podcastTitle, p.artworkUrl AS podcastArtworkUrl
    FROM episodes e
    INNER JOIN podcasts p ON e.podcastId = p.id
    ORDER BY e.publishedAt DESC
    LIMIT :limit
    """

// One row per subscription: its newest-published episode (joined to the podcast's title/artwork),
// subscriptions ordered by that episode's date. The correlated MAX keeps a single row per podcast.
internal const val SUBSCRIPTION_LATEST_EPISODES_QUERY =
    """
    SELECT e.id, e.podcastId, e.title, e.description, e.enclosureUrl, e.artworkUrl,
        e.publishedAt, e.durationMs,
        p.title AS podcastTitle, p.artworkUrl AS podcastArtworkUrl
    FROM episodes e
    INNER JOIN podcasts p ON e.podcastId = p.id
    WHERE e.publishedAt = (SELECT MAX(e2.publishedAt) FROM episodes e2 WHERE e2.podcastId = e.podcastId)
    GROUP BY e.podcastId
    ORDER BY e.publishedAt DESC
    """

@Dao
interface EpisodeDao {
    @Upsert
    suspend fun upsertAll(episodes: List<EpisodeEntity>)

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY publishedAt DESC")
    fun observeByPodcast(podcastId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE id = :id")
    fun observeById(id: String): Flow<EpisodeEntity?>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun findById(id: String): EpisodeEntity?

    @Query(RECENT_EPISODES_QUERY)
    fun observeRecent(limit: Int = 30): Flow<List<RecentEpisodeView>>

    @Query(SUBSCRIPTION_LATEST_EPISODES_QUERY)
    fun observeLatestPerSubscription(): Flow<List<SubscriptionLatestEpisodeView>>
}
