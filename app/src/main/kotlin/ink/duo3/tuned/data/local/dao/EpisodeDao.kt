package ink.duo3.tuned.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ink.duo3.tuned.data.local.entity.EpisodeEntity
import ink.duo3.tuned.data.local.entity.RecentEpisodeView
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
}
