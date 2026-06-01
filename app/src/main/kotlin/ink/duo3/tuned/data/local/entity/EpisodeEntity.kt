package ink.duo3.tuned.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * [id] is derived from the feed-provided [guid] when present, otherwise from a
 * fallback ladder (see FeedIdentity). [guid] is retained nullable so refreshes can
 * distinguish "feed had no guid" from a recomputed identity.
 */
@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = PodcastEntity::class,
            parentColumns = ["id"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("podcastId")],
)
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val podcastId: String,
    val guid: String?,
    val enclosureUrl: String,
    val publishedAt: Long,
    val durationMs: Long?,
    val title: String? = null,
    val description: String? = null,
)
