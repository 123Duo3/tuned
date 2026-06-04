package ink.duo3.tuned.player.media3

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Recovers playback from transient network errors. ExoPlayer stops at [Player.STATE_IDLE] on a
 * source error and does not retry on its own, so a brief connectivity drop would otherwise leave
 * the user stuck on a dead stream. This listens for network-class [PlaybackException]s and
 * re-prepares the player after a [PlaybackRetryPolicy] backoff — re-preparing resumes from the
 * last position. Non-network errors and a spent retry budget are left surfaced.
 *
 * A queued re-prepare is only carried out if, once the backoff elapses, the user still wants to
 * play the same item — and any pending retry is cancelled outright when the user pauses/stops,
 * when playback recovers some other way ([Player.STATE_READY]), or when the episode changes. That
 * keeps a stale error from one item from interrupting a fresh, healthy playback. The budget resets
 * on recovery and on episode switch.
 */
@OptIn(UnstableApi::class)
internal class PlaybackErrorRecovery(
    private val player: Player,
    private val policy: PlaybackRetryPolicy = PlaybackRetryPolicy(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var retryJob: Job? = null

    private val listener =
        object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                // Only the user's intent to play warrants a retry, and only for network drops;
                // a paused stream or a permanent error is left as-is.
                val recoverable = player.playWhenReady && error.errorCode in RETRIABLE_CODES
                val backoff = (if (recoverable) policy.nextBackoffMs() else null) ?: return
                val target = player.currentMediaItem?.mediaId
                cancelPendingRetry()
                retryJob =
                    scope.launch {
                        delay(backoff)
                        // State may have moved during the backoff: only re-prepare if the user
                        // still wants to play, and still the same item this error came from.
                        if (player.playWhenReady && player.currentMediaItem?.mediaId == target) {
                            player.prepare()
                        }
                    }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                // Recovered (by our retry or otherwise): refresh the budget and drop any stale retry.
                if (playbackState == Player.STATE_READY) {
                    policy.reset()
                    cancelPendingRetry()
                }
            }

            override fun onPlayWhenReadyChanged(
                playWhenReady: Boolean,
                reason: Int,
            ) {
                // User paused/stopped: abandon recovery rather than yanking playback back later.
                if (!playWhenReady) cancelPendingRetry()
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int,
            ) {
                // Switched episodes: a retry queued for the old item is moot; start fresh.
                cancelPendingRetry()
                policy.reset()
            }
        }

    fun attach() = player.addListener(listener)

    fun detach() {
        cancelPendingRetry()
        player.removeListener(listener)
        scope.cancel()
    }

    private fun cancelPendingRetry() {
        retryJob?.cancel()
        retryJob = null
    }

    private companion object {
        val RETRIABLE_CODES =
            setOf(
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            )
    }
}
