package ink.duo3.tuned.domain.model

/**
 * Playback progress for one episode. [positionMs] is the resume point; [completed] is
 * set when the episode played to the end (the UI resumes such episodes from the start).
 */
data class EpisodeProgress(
    val episodeId: String,
    val positionMs: Long,
    val completed: Boolean,
    val lastPlayedAt: Long,
)
