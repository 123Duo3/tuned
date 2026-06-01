package ink.duo3.tuned.domain.model

/**
 * A subscribed podcast as the UI consumes it — pure Kotlin, no Room or network
 * types. [id] is the stable feed-derived identity; [feedUrl] is the current
 * (post-redirect) feed location. Display fields are nullable because a feed may
 * omit any of them.
 */
data class Podcast(
    val id: String,
    val feedUrl: String,
    val title: String?,
    val author: String?,
    val description: String?,
    val artworkUrl: String?,
)
