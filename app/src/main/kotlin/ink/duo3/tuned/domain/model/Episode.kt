package ink.duo3.tuned.domain.model

/**
 * An episode as the UI consumes it — pure Kotlin, no Room or network types.
 * [enclosureUrl] is always present (the mapper drops items without playable audio);
 * [publishedAtMs] is epoch millis (0 when the feed gave no usable date).
 */
data class Episode(
    val id: String,
    val podcastId: String,
    val title: String?,
    val description: String?,
    val enclosureUrl: String,
    val publishedAtMs: Long,
    val durationMs: Long?,
)
