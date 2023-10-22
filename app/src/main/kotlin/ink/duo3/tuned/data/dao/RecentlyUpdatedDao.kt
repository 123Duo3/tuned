package ink.duo3.tuned.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ink.duo3.tuned.data.entity.RecUpdPodEntity

@Dao
interface RecentlyUpdatedDao {

    @Insert
    fun insertAll(list: List<RecUpdPodEntity>)

    @Query("SELECT * FROM RecentlyUpdatedPodcast")
    fun getRecentlyUpdatedEpisodes(): List<RecUpdPodEntity>

    @Query("DELETE FROM RecentlyUpdatedPodcast")
    fun deleteAll()
}