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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
    private val _audioLevelBars = MutableSharedFlow<List<Float>>(replay = 1)
    override val audioLevelBars: Flow<List<Float>> = _audioLevelBars

    private val controllerFuture: ListenableFuture<MediaController> =
        MediaController
            .Builder(appContext, SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java)))
            .buildAsync()
    private var controller: MediaController? = null
    private var ticker: Job? = null
    private var audioLevelTicker: Job? = null
    private var sleepTimerJob: Job? = null
    private var pendingPlayback: PendingPlayback? = null
    private var knownPlaybackDuration: KnownPlaybackDuration? = null

    // elapsedRealtime() instant the timer fires; null when no timer is armed. Wall-clock
    // based so it counts down even while the screen is off and survives manual pauses.
    private var sleepTimerEndMs: Long? = null

    init {
        controllerFuture.addListener({
            val ready = controllerFuture.get()
            controller = ready
            _audioLevelBars.tryEmit(emptyList())
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
                        updateAudioLevelTicker()
                    }
                },
            )
            scope.launch {
                _audioLevelBars
                    .subscriptionCount
                    .map { it > 0 }
                    .distinctUntilChanged()
                    .collect { updateAudioLevelTicker() }
            }
            pushState()
            // After a cold start (process/service killed) the player is empty; reload the
            // last episode paused at its saved position so the mini-player returns and the
            // user can resume. A live, still-loaded session is left untouched.
            if (ready.currentMediaItem == null) {
                scope.launch {
                    val item = resumptionSource.lastPlayable() ?: return@launch
                    if (ready.currentMediaItem != null) return@launch
                    rememberKnownDuration(item)
                    startPendingPlayback(item, positionMs = item.startPositionMs ?: 0L, buffering = false)
                    ready.setMediaItem(item.toMediaItem(), item.startPositionMs ?: 0L)
                    ready.prepare()
                }
            }
        }, MoreExecutors.directExecutor())
    }

    override fun play(item: PlayableEpisode) {
        command {
            // An explicit start position (e.g. a tapped show-notes timestamp, including 0) is
            // authoritative; null falls back to the saved resume point.
            val startMs = item.startPositionMs ?: progressRepository.resumePositionMs(item.episodeId)
            rememberKnownDuration(item)
            startPendingPlayback(item, positionMs = startMs)
            setMediaItem(item.toMediaItem(), startMs)
            prepare()
            play()
            pushState()
        }
    }

    override fun resume() {
        pushResumeRequestedState()
        command { play() }
    }

    override fun pause() {
        pushPauseRequestedState()
        command { pause() }
    }

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
        pendingPlayback = null
        knownPlaybackDuration = null
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
        pushState(base)
    }

    private fun pushResumeRequestedState() {
        _state.update { state ->
            state
                .takeIf { it.episodeId != null }
                ?.copy(isPlaying = true, buffering = false, sleepTimerRemainingMs = sleepTimerRemainingMs())
                ?: state
        }
    }

    private fun pushPauseRequestedState() {
        _state.update { state ->
            state.copy(isPlaying = false, buffering = false, sleepTimerRemainingMs = sleepTimerRemainingMs())
        }
    }

    private fun startPendingPlayback(
        item: PlayableEpisode,
        positionMs: Long,
        buffering: Boolean = true,
    ) {
        val snapshot =
            PlaybackState(
                episodeId = item.episodeId,
                title = item.title,
                podcastTitle = item.podcastTitle,
                artworkUrl = item.artworkUrl,
                positionMs = positionMs.coerceAtLeast(0L),
                durationMs = item.durationMs?.takeIf { it > 0L },
                buffering = buffering,
            )
        pendingPlayback =
            PendingPlayback(
                snapshot = snapshot,
                positionMs = positionMs.coerceAtLeast(0L),
                createdAtElapsedMs = SystemClock.elapsedRealtime(),
            )
        _state.value =
            snapshot.copy(
                sleepTimerRemainingMs = sleepTimerRemainingMs(),
            )
    }

    private fun sleepTimerRemainingMs(): Long? =
        sleepTimerEndMs?.let { end ->
            (end - SystemClock.elapsedRealtime()).coerceAtLeast(0)
        }

    private fun pushState(base: PlaybackState) {
        val stableBase =
            base
                .withKnownPlaybackDuration()
                .withPendingPlaybackSnapshot()
        _state.value =
            stableBase.copy(
                sleepTimerRemainingMs = sleepTimerRemainingMs(),
            )
    }

    private fun PlaybackState.withPendingPlaybackSnapshot(): PlaybackState {
        val pending = pendingPlayback ?: return this
        val insideStartupGrace =
            SystemClock.elapsedRealtime() - pending.createdAtElapsedMs <= PENDING_POSITION_GRACE_MS
        val matchesPendingEpisode = episodeId == pending.snapshot.episodeId
        return when {
            !matchesPendingEpisode && insideStartupGrace -> pending.snapshot
            !matchesPendingEpisode -> {
                pendingPlayback = null
                this
            }
            shouldKeepPendingPosition(pending, insideStartupGrace) -> copy(positionMs = pending.positionMs)
            else -> {
                pendingPlayback = null
                this
            }
        }
    }

    private fun PlaybackState.shouldKeepPendingPosition(
        pending: PendingPlayback,
        insideStartupGrace: Boolean,
    ): Boolean =
        !isSettledAtPendingPosition(pending.positionMs) &&
            (buffering || !isPlaying || insideStartupGrace)

    private fun PlaybackState.isSettledAtPendingPosition(positionMs: Long): Boolean =
        this.positionMs - positionMs in 0L..PENDING_POSITION_SETTLED_TOLERANCE_MS

    private fun rememberKnownDuration(item: PlayableEpisode) {
        knownPlaybackDuration =
            KnownPlaybackDuration(
                episodeId = item.episodeId,
                durationMs = item.durationMs?.takeIf { it > 0L },
            )
    }

    private fun PlaybackState.withKnownPlaybackDuration(): PlaybackState {
        val known = knownPlaybackDuration
        val knownDuration = known?.durationMs
        val shouldUseKnownDuration =
            knownDuration != null &&
                episodeId == known.episodeId &&
                durationMs.isUnknownDuration()
        if (known != null && episodeId != null && episodeId != known.episodeId) {
            knownPlaybackDuration = null
        }
        return if (shouldUseKnownDuration) {
            copy(durationMs = knownDuration)
        } else {
            this
        }
    }

    private fun updateAudioLevelTicker() {
        val shouldRun = _audioLevelBars.subscriptionCount.value > 0 && controller?.isPlaying == true
        if (shouldRun) {
            if (audioLevelTicker?.isActive == true) return
            PlaybackAudioLevelMeter.setEnabled(true)
            audioLevelTicker =
                scope.launch {
                    while (isActive) {
                        _audioLevelBars.emit(PlaybackAudioLevelMeter.snapshot())
                        delay(AUDIO_LEVEL_TICK_MS)
                    }
                }
        } else {
            audioLevelTicker?.cancel()
            audioLevelTicker = null
            PlaybackAudioLevelMeter.setEnabled(false)
            _audioLevelBars.tryEmit(emptyList())
        }
    }

    private companion object {
        const val TICK_MS = 500L
        const val AUDIO_LEVEL_TICK_MS = 35L
        const val SLEEP_TICK_MS = 1_000L
        const val PENDING_POSITION_GRACE_MS = 2_000L
        const val PENDING_POSITION_SETTLED_TOLERANCE_MS = 2_000L
    }
}

private data class PendingPlayback(
    val snapshot: PlaybackState,
    val positionMs: Long,
    val createdAtElapsedMs: Long,
)

private data class KnownPlaybackDuration(
    val episodeId: String,
    val durationMs: Long?,
)

private fun Long?.isUnknownDuration(): Boolean = this == null || this <= 0L

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
        durationMs = duration.takeUnless { it == C.TIME_UNSET }?.coerceAtLeast(0),
        speed = playbackParameters.speed,
        buffering = playWhenReady && playbackState == Player.STATE_BUFFERING,
    )
}
