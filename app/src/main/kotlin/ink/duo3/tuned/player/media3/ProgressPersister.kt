package ink.duo3.tuned.player.media3

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import ink.duo3.tuned.domain.player.isPlaybackComplete
import ink.duo3.tuned.domain.repository.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Persists resume positions for the service's player. Saves periodically while playing
 * and flushes on pause, episode switch, end-of-episode, and teardown so progress is never
 * lost across backgrounding or process death.
 *
 * [completed] is derived from the live player state ([Player.STATE_ENDED]) on every save
 * rather than passed in, so the end-of-episode flag can't be downgraded by a later pause /
 * teardown save that races the same item. Episode switches are caught via
 * [Player.Listener.onPositionDiscontinuity], which carries the outgoing item's last position.
 */
@OptIn(UnstableApi::class)
internal class ProgressPersister(
    private val player: Player,
    private val repository: ProgressRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ticker: Job? = null
    private val playbackDurationByEpisodeId = mutableMapOf<String, Long>()

    private val listener =
        object : Player.Listener {
            override fun onEvents(
                player: Player,
                events: Player.Events,
            ) {
                rememberMeasuredDuration()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                rememberMeasuredDuration()
                if (isPlaying) {
                    startTicker()
                } else {
                    stopTicker()
                    save()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                rememberMeasuredDuration()
                if (playbackState == Player.STATE_ENDED) {
                    stopTicker()
                    save()
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                val outgoing = oldPosition.mediaItem?.mediaId?.takeIf { it.isNotEmpty() } ?: return
                // Same item => a seek, not a switch; the ticker already covers it.
                if (outgoing == newPosition.mediaItem?.mediaId) return
                scope.launch {
                    repository.save(
                        episodeId = outgoing,
                        positionMs = oldPosition.positionMs.coerceAtLeast(0),
                        completed = false,
                        playbackDurationMs = measuredDurationMs(outgoing),
                    )
                }
            }
        }

    fun attach() = player.addListener(listener)

    /** Final synchronous flush before the player is released and the scope dies. */
    fun detachAndFlush() {
        stopTicker()
        player.removeListener(listener)
        val episodeId =
            currentEpisodeId() ?: run {
                scope.cancel()
                return
            }
        runBlocking {
            repository.save(
                episodeId = episodeId,
                positionMs = position(),
                completed = isCompleted(),
                playbackDurationMs = measuredDurationMs(episodeId),
            )
        }
        scope.cancel()
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker =
            scope.launch {
                while (isActive) {
                    delay(SAVE_INTERVAL_MS)
                    save()
                }
            }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun save() {
        val episodeId = currentEpisodeId() ?: return
        val completed = isCompleted()
        val positionMs = position()
        scope.launch {
            repository.save(
                episodeId = episodeId,
                positionMs = positionMs,
                completed = completed,
                playbackDurationMs = measuredDurationMs(episodeId),
            )
        }
    }

    private fun position(): Long = player.currentPosition.coerceAtLeast(0)

    private fun measuredDurationMs(episodeId: String): Long? =
        playbackDurationByEpisodeId[episodeId]
            ?: if (currentEpisodeId() == episodeId) {
                rememberMeasuredDuration()
            } else {
                null
            }

    private fun rememberMeasuredDuration(): Long? {
        val episodeId = currentEpisodeId()
        val durationMs =
            player.duration
                .takeUnless { it == C.TIME_UNSET }
                ?.takeIf { it > 0L }
        if (episodeId != null && durationMs != null) {
            playbackDurationByEpisodeId[episodeId] = durationMs
        }
        return durationMs
    }

    private fun isCompleted(): Boolean =
        isPlaybackComplete(
            positionMs = position(),
            durationMs = player.duration.takeUnless { it == C.TIME_UNSET },
            playbackEnded = player.playbackState == Player.STATE_ENDED,
        )

    private fun currentEpisodeId(): String? = player.currentMediaItem?.mediaId?.takeIf { it.isNotEmpty() }

    private companion object {
        const val SAVE_INTERVAL_MS = 10_000L
    }
}
