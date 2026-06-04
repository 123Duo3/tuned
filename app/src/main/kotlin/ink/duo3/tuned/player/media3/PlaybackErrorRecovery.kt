package ink.duo3.tuned.player.media3

import androidx.annotation.OptIn
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
 * last position. Non-network errors and a spent retry budget are left surfaced. Recovery is
 * abandoned if the user has paused (`playWhenReady == false`); the budget resets once playback
 * reaches [Player.STATE_READY] again.
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
                retryJob?.cancel()
                retryJob =
                    scope.launch {
                        delay(backoff)
                        player.prepare()
                    }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) policy.reset()
            }
        }

    fun attach() = player.addListener(listener)

    fun detach() {
        retryJob?.cancel()
        player.removeListener(listener)
        scope.cancel()
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
