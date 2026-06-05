package ink.duo3.tuned.domain.player

import kotlinx.coroutines.flow.StateFlow

/**
 * The app's playback abstraction. The ONLY playback surface the rest of the app
 * talks to — the Media3 implementation lives in `player/media3/` and is the only
 * place importing androidx.media3.* (enforced by Konsist).
 */
interface PlaybackController {
    val state: StateFlow<PlaybackState>

    fun play(item: PlayableEpisode)

    fun resume()

    fun pause()

    fun seekTo(positionMs: Long)

    fun seekBy(deltaMs: Long)

    fun setSpeed(speed: Float)

    fun stop()

    /** Pauses playback after [durationMs]; replaces any running timer. */
    fun startSleepTimer(durationMs: Long)

    fun cancelSleepTimer()
}

/**
 * Minimal data the player needs to start playback of an episode. [startPositionMs] is an
 * explicit start point (e.g. a tapped show-notes timestamp, including 0 for "from the
 * beginning"); null means start from the saved resume position.
 */
data class PlayableEpisode(
    val episodeId: String,
    val title: String,
    val podcastTitle: String,
    val artworkUrl: String?,
    val streamUrl: String,
    val startPositionMs: Long? = null,
)

/**
 * The whole UI-visible playback snapshot. Display fields ([title], [podcastTitle],
 * [artworkUrl]) are carried here — mirrored from the player's current item metadata — so
 * the player screen and mini-player render from this state alone, with no DB round-trip.
 * [episodeId] is null when nothing is loaded (render no mini-player).
 */
data class PlaybackState(
    val episodeId: String? = null,
    val title: String? = null,
    val podcastTitle: String? = null,
    val artworkUrl: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
    val buffering: Boolean = false,
    /** Milliseconds until the sleep timer pauses playback, or null when no timer is set. */
    val sleepTimerRemainingMs: Long? = null,
)
