package ink.duo3.tuned.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ink.duo3.tuned.data.dao.RecentlyUpdatedDao
import ink.duo3.tuned.data.dao.SubscriptionDao
import ink.duo3.tuned.data.entity.RecUpdPodEntity
import ink.duo3.tuned.data.entity.SubscribedPodEntity

@Database(
    entities = [SubscribedPodEntity::class, RecUpdPodEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TunedDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun recentlyUpdatedDao(): RecentlyUpdatedDao
}