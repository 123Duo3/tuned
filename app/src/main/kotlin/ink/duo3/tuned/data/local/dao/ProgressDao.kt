package ink.duo3.tuned.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ink.duo3.tuned.data.local.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Upsert
    suspend fun upsert(progress: ProgressEntity)

    @Query("SELECT * FROM progress WHERE episodeId = :episodeId")
    suspend fun findByEpisode(episodeId: String): ProgressEntity?

    @Query("SELECT * FROM progress WHERE episodeId = :episodeId")
    fun observeByEpisode(episodeId: String): Flow<ProgressEntity?>

    @Query("SELECT * FROM progress WHERE completed = 0 ORDER BY lastPlayedAt DESC LIMIT 1")
    suspend fun mostRecent(): ProgressEntity?
}
