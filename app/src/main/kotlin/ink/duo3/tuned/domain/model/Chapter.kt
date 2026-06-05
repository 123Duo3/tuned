package ink.duo3.tuned.domain.model

/**
 * A single chapter within an episode, sourced from either a Podcasting 2.0 chapters
 * JSON document or embedded ID3 chapters. [startTimeMs] is the offset from the episode
 * start; the chapter is "current" while playback sits between its start and the next
 * chapter's. [imageUrl] is the chapter's own artwork — shown in place of the episode art
 * while the chapter is active — and [url] an optional related link.
 */
data class Chapter(
    val startTimeMs: Long,
    val title: String?,
    val imageUrl: String? = null,
    val url: String? = null,
)
