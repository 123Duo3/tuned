package ink.duo3.tuned.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ink.duo3.tuned.data.local.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {
    @Upsert
    suspend fun upsertAll(episodes: List<EpisodeEntity>)

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY publishedAt DESC")
    fun observeByPodcast(podcastId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun findById(id: String): EpisodeEntity?
}
