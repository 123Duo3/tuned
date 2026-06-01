package ink.duo3.tuned.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ink.duo3.tuned.data.local.entity.PodcastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    @Upsert
    suspend fun upsert(podcast: PodcastEntity)

    @Query("SELECT * FROM podcasts ORDER BY lastFetchedAt DESC")
    fun observeAll(): Flow<List<PodcastEntity>>

    @Query("SELECT * FROM podcasts WHERE id = :id")
    fun observeById(id: String): Flow<PodcastEntity?>

    @Query("SELECT * FROM podcasts WHERE id = :id")
    suspend fun findById(id: String): PodcastEntity?

    @Query("DELETE FROM podcasts WHERE id = :id")
    suspend fun deleteById(id: String)
}
