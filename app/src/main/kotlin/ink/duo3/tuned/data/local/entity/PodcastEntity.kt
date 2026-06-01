package ink.duo3.tuned.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Identity + synchronization fields, plus the display fields the library UI renders.
 * The display columns were added in schema v2 (see MIGRATION_1_2); they are nullable
 * because a feed may omit any of them.
 *
 * [id] is a stable opaque key derived from the canonical feed URL; [canonicalFeedUrl]
 * is kept separately because [currentFeedUrl] may diverge after a redirect.
 */
@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey val id: String,
    val canonicalFeedUrl: String,
    val currentFeedUrl: String,
    val etag: String?,
    val lastModified: String?,
    val lastFetchedAt: Long,
    val title: String? = null,
    val author: String? = null,
    val description: String? = null,
    val artworkUrl: String? = null,
)
