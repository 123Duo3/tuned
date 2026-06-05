package ink.duo3.tuned.domain.model

/**
 * An episode as the UI consumes it — pure Kotlin, no Room or network types.
 * [enclosureUrl] is null when the item carries no audio (e.g. a text-only
 * announcement); such items are still listed, just not playable.
 * [publishedAtMs] is epoch millis (0 when the feed gave no usable date).
 * [artworkUrl] is the item's own episode art when the feed supplies it; callers fall
 * back to the podcast's artwork when it is null.
 * [chaptersUrl] is the Podcasting 2.0 `<podcast:chapters>` JSON document URL when the
 * item declares one; null otherwise (the player then falls back to embedded ID3 chapters).
 */
data class Episode(
    val id: String,
    val podcastId: String,
    val title: String?,
    val description: String?,
    val enclosureUrl: String?,
    val artworkUrl: String?,
    val publishedAtMs: Long,
    val durationMs: Long?,
    val chaptersUrl: String? = null,
)
