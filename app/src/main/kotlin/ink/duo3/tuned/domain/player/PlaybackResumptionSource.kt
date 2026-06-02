package ink.duo3.tuned.domain.player

/**
 * Supplies the episode the player should restore after the app's process is killed and
 * relaunched: the most recently played, unfinished episode, as a [PlayableEpisode] whose
 * [PlayableEpisode.startPositionMs] is its saved resume point. Returns null when there is
 * nothing to resume (no history, or the last episode was completed or has since gone).
 */
interface PlaybackResumptionSource {
    suspend fun lastPlayable(): PlayableEpisode?
}
