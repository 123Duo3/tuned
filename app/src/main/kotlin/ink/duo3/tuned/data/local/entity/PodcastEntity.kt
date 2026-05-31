package ink.duo3.tuned.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Identity + synchronization fields only (see CLAUDE.md). Display fields (title,
 * artwork) arrive with the library UI as a deliberate schema migration.
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
)
