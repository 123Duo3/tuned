package ink.duo3.tuned.domain.player

/**
 * Playback state for an episode entry point. [progress] is clamped to 0..1 and is
 * independent from the label: a currently loaded but still-at-start episode can be playing
 * with zero progress.
 */
data class EpisodePlaybackSnapshot(
    val status: EpisodePlaybackStatus = EpisodePlaybackStatus.Unplayed,
    val progress: Float = 0f,
    val remainingMs: Long? = null,
    val positionMs: Long = 0L,
    val durationMs: Long? = null,
)

enum class EpisodePlaybackStatus {
    Unplayed,
    Loading,
    Playing,
    Resume,
    Completed,
}
