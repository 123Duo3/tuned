package ink.duo3.tuned.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ink.duo3.tuned.data.entity.SubscribedPodEntity

@Dao
interface SubscriptionDao {

    @Upsert
    fun subscribe(subscribedPodEntity: SubscribedPodEntity)

    @Query("SELECT * FROM SubscribedPodcast")
    fun getSubscriptions(): List<SubscribedPodEntity>

    @Query("DELETE FROM subscribedpodcast WHERE id = :id")
    fun unSubscribe(id: Int)

}