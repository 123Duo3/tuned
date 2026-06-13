package ink.duo3.tuned.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ink.duo3.tuned.data.local.dao.EpisodeDao
import ink.duo3.tuned.data.local.dao.PodcastDao
import ink.duo3.tuned.data.local.dao.ProgressDao
import ink.duo3.tuned.data.local.entity.EpisodeEntity
import ink.duo3.tuned.data.local.entity.PodcastEntity
import ink.duo3.tuned.data.local.entity.ProgressEntity

@Database(
    entities = [
        PodcastEntity::class,
        EpisodeEntity::class,
        ProgressEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class TunedDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao

    abstract fun episodeDao(): EpisodeDao

    abstract fun progressDao(): ProgressDao
}
