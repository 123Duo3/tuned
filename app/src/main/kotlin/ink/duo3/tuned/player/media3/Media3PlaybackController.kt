package ink.duo3.tuned.player.media3

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import ink.duo3.tuned.domain.player.PlayableEpisode
import ink.duo3.tuned.domain.player.PlaybackController
import ink.duo3.tuned.domain.player.PlaybackResumptionSource
import ink.duo3.tuned.domain.player.PlaybackState
import ink.duo3.tuned.domain.repository.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The app's single [PlaybackController], backed by a Media3 [MediaController] that talks
 * to [PlaybackService]. This and the service are the only classes importing media3.
 *
 * State is mirrored into [state] from the connected controller: a [Player.Listener]
 * pushes on every event, and a ticker re-pushes position while playing so the UI slider
 * advances. Resume points come from [ProgressRepository]; the service owns persistence.
 *
 * Suppresses TooManyFunctions: implementing the 9-method [PlaybackController] interface
 * plus two small private helpers edges past the default gate, but the class is single-purpose.
 */
@Suppress("TooManyFunctions")
@OptIn(UnstableApi::class)
class Media3PlaybackController(
    private val appContext: Context,
    private val progressRepository: ProgressRepository,
    private val resumptionSource: PlaybackResumptionSource,
) : PlaybackController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val controllerFuture: ListenableFuture<MediaController> =
        MediaController
            .Builder(appContext, SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java)))
            .buildAsync()
    private var controller: MediaController? = null
    private var ticker: Job? = null
    private var sleepTimerJob: Job? = null

    // elapsedRealtime() instant the timer fires; null when no timer is armed. Wall-clock
    // based so it counts down even while the screen is off and survives manual pauses.
    private var sleepTimerEndMs: Long? = null

    init {
        controllerFuture.addListener({
            val ready = controllerFuture.get()
            controller = ready
            ready.addListener(
                object : Player.Listener {
                    override fun onEvents(
                        player: Player,
                        events: Player.Events,
                    ) {
                        pushState()
                        // Run a position ticker only while playing so the UI slider advances;
                        // stop it on pause/end so the coroutine isn't left spinning.
                        if (!player.isPlaying) {
                            ticker?.cancel()
                            ticker = null
                        } else if (ticker?.isActive != true) {
                            ticker =
                                scope.launch {
                                    while (isActive) {
                                        pushState()
                                        delay(TICK_MS)
                                    }
                                }
                        }
                    }
                },
            )
            pushState()
            // After a cold start (process/service killed) the player is empty; reload the
            // last episode paused at its saved position so the mini-player returns and the
            // user can resume. A live, still-loaded session is left untouched.
            if (ready.currentMediaItem == null) {
                scope.launch {
                    val item = resumptionSource.lastPlayable() ?: return@launch
                    if (ready.currentMediaItem != null) return@launch
                    ready.setMediaItem(item.toMediaItem(), item.startPositionMs)
                    ready.prepare()
                }
            }
        }, MoreExecutors.directExecutor())
    }

    override fun play(item: PlayableEpisode) {
        command {
            val resumeMs = progressRepository.resumePositionMs(item.episodeId).coerceAtLeast(item.startPositionMs)
            setMediaItem(item.toMediaItem(), resumeMs)
            prepare()
            play()
        }
    }

    override fun resume() = command { play() }

    override fun pause() = command { pause() }

    override fun seekTo(positionMs: Long) = command { seekTo(positionMs.coerceAtLeast(0)) }

    override fun seekBy(deltaMs: Long) =
        command {
            val target = currentPosition + deltaMs
            val max = if (duration == C.TIME_UNSET) target else duration
            seekTo(target.coerceIn(0, max.coerceAtLeast(0)))
        }

    override fun setSpeed(speed: Float) = command { setPlaybackSpeed(speed) }

    override fun stop() {
        cancelSleepTimer()
        command {
            stop()
            clearMediaItems()
        }
    }

    override fun startSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        if (durationMs <= 0) {
            cancelSleepTimer()
            return
        }
        val end = SystemClock.elapsedRealtime() + durationMs
        sleepTimerEndMs = end
        sleepTimerJob =
            scope.launch {
                while (isActive) {
                    if (SystemClock.elapsedRealtime() >= end) {
                        controller?.pause()
                        sleepTimerEndMs = null
                        sleepTimerJob = null
                        pushState()
                        break
                    }
                    pushState()
                    delay(SLEEP_TICK_MS)
                }
            }
        pushState()
    }

    override fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepTimerEndMs = null
        pushState()
    }

    /** Runs [block] on the controller once it's connected, or queues it on the connect future. */
    private fun command(block: suspend MediaController.() -> Unit) {
        val ready = controller
        if (ready != null) {
            scope.launch { ready.block() }
        } else {
            controllerFuture.addListener(
                { scope.launch { controllerFuture.get().block() } },
                MoreExecutors.directExecutor(),
            )
        }
    }

    private fun pushState() {
        val base = controller?.toPlaybackState() ?: PlaybackState()
        val remaining = sleepTimerEndMs?.let { (it - SystemClock.elapsedRealtime()).coerceAtLeast(0) }
        _state.value = base.copy(sleepTimerRemainingMs = remaining)
    }

    private companion object {
        const val TICK_MS = 500L
        const val SLEEP_TICK_MS = 1_000L
    }
}

/** Domain episode -> Media3 item, carrying display metadata so the UI renders from state alone. */
@OptIn(UnstableApi::class)
internal fun PlayableEpisode.toMediaItem(): MediaItem =
    MediaItem
        .Builder()
        .setMediaId(episodeId)
        .setUri(streamUrl)
        .setMediaMetadata(
            MediaMetadata
                .Builder()
                .setTitle(title)
                .setArtist(podcastTitle)
                .setArtworkUri(artworkUrl?.let(android.net.Uri::parse))
                .build(),
        ).build()

/** Snapshot the current player into the UI-visible [PlaybackState]. */
@OptIn(UnstableApi::class)
internal fun MediaController.toPlaybackState(): PlaybackState {
    val item = currentMediaItem ?: return PlaybackState()
    val metadata = item.mediaMetadata
    return PlaybackState(
        episodeId = item.mediaId.ifEmpty { null },
        title = metadata.title?.toString(),
        podcastTitle = metadata.artist?.toString(),
        artworkUrl = metadata.artworkUri?.toString(),
        isPlaying = isPlaying,
        positionMs = currentPosition.coerceAtLeast(0),
        durationMs = if (duration == C.TIME_UNSET) 0L else duration.coerceAtLeast(0),
        speed = playbackParameters.speed,
        buffering = playbackState == Player.STATE_BUFFERING,
    )
}
