package ink.duo3.tuned.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * One row per episode; [episodeId] is both PK and FK so progress is deleted with
 * its episode. [positionMs] is the resume point persisted during playback.
 */
@Entity(
    tableName = "progress",
    foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ProgressEntity(
    @PrimaryKey val episodeId: String,
    val positionMs: Long,
    val completed: Boolean,
    val lastPlayedAt: Long,
    val playbackDurationMs: Long? = null,
)
