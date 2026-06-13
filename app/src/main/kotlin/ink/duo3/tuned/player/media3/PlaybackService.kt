package ink.duo3.tuned.player.media3

import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import ink.duo3.tuned.R
import ink.duo3.tuned.domain.player.PlaybackSpeeds
import ink.duo3.tuned.domain.repository.ProgressRepository
import org.koin.android.ext.android.inject

/**
 * The media playback foreground service. Owns the [ExoPlayer] and a [MediaSession] so
 * playback survives backgrounding, task removal, and continues from the notification /
 * lockscreen. [ProgressPersister] writes resume positions to [ProgressRepository].
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private val progressRepository: ProgressRepository by inject()

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var persister: ProgressPersister? = null
    private var errorRecovery: PlaybackErrorRecovery? = null

    override fun onCreate() {
        super.onCreate()
        setForegroundServiceTimeoutMs(INACTIVE_NOTIFICATION_TIMEOUT_MS)
        setMediaNotificationProvider(playbackNotificationProvider())
        val exo =
            ExoPlayer
                .Builder(this, audioLevelRenderersFactory())
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                        .build(),
                    // handleAudioFocus =
                    true,
                ).setHandleAudioBecomingNoisy(true)
                .build()
        player = exo
        mediaSession =
            MediaSession
                .Builder(this, exo)
                .setCallback(notificationActionCallback())
                .setMediaButtonPreferences(notificationMediaButtons(exo))
                .build()
        exo.addListener(
            object : Player.Listener {
                override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                    mediaSession?.setMediaButtonPreferences(notificationMediaButtons(exo))
                }
            },
        )
        persister = ProgressPersister(exo, progressRepository).also { it.attach() }
        errorRecovery = PlaybackErrorRecovery(exo).also { it.attach() }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val exo = player
        if (exo == null || !exo.playWhenReady || exo.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        errorRecovery?.detach()
        persister?.detachAndFlush()
        PlaybackAudioLevelMeter.clear()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        persister = null
        errorRecovery = null
        super.onDestroy()
    }

    private fun audioLevelRenderersFactory(): DefaultRenderersFactory =
        object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioOutputPlaybackParams: Boolean,
            ): AudioSink =
                DefaultAudioSink
                    .Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams)
                    .setAudioProcessors(arrayOf(TeeAudioProcessor(PlaybackAudioLevelMeter)))
                    .build()
        }

    private fun playbackNotificationProvider(): DefaultMediaNotificationProvider =
        DefaultMediaNotificationProvider
            .Builder(this)
            .build()
            .also { it.setSmallIcon(R.drawable.ic_notification_small) }

    private fun notificationActionCallback(): MediaSession.Callback =
        object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
            ): MediaSession.ConnectionResult {
                val sessionCommands =
                    SessionCommands
                        .Builder()
                        .addSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.commands)
                        .add(SKIP_BACK_COMMAND)
                        .add(SKIP_FORWARD_COMMAND)
                        .add(CYCLE_SPEED_COMMAND)
                        .add(NEXT_COMMAND)
                        .build()

                return MediaSession
                    .ConnectionResult
                    .AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setMediaButtonPreferences(notificationMediaButtons(session.player))
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle,
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    ACTION_SKIP_BACK -> session.player.seekBy(SKIP_BACK_MS)
                    ACTION_SKIP_FORWARD -> session.player.seekBy(SKIP_FORWARD_MS)
                    ACTION_CYCLE_SPEED -> session.player.cyclePlaybackSpeed()
                    ACTION_NEXT -> if (session.player.hasNextMediaItem()) session.player.seekToNextMediaItem()
                    else -> return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
                }

                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

    private fun notificationMediaButtons(player: Player): List<CommandButton> {
        val currentSpeed = player.playbackParameters.speed
        val speedLabel = PlaybackSpeeds.label(currentSpeed)
        val speedIcon = SPEED_ICONS.getValue(PlaybackSpeeds.closestPreset(currentSpeed))

        return listOf(
            CommandButton
                .Builder(CommandButton.ICON_SKIP_BACK_15)
                .setSessionCommand(SKIP_BACK_COMMAND)
                .setDisplayName(getString(R.string.player_skip_back))
                .setSlots(CommandButton.SLOT_BACK)
                .build(),
            CommandButton
                .Builder(CommandButton.ICON_SKIP_FORWARD_30)
                .setSessionCommand(SKIP_FORWARD_COMMAND)
                .setDisplayName(getString(R.string.player_skip_forward))
                .setSlots(CommandButton.SLOT_FORWARD)
                .build(),
            CommandButton
                .Builder(CommandButton.ICON_UNDEFINED)
                .setCustomIconResId(speedIcon)
                .setSessionCommand(CYCLE_SPEED_COMMAND)
                .setDisplayName(getString(R.string.player_speed, speedLabel))
                .setSlots(CommandButton.SLOT_OVERFLOW)
                .build(),
            CommandButton
                .Builder(CommandButton.ICON_NEXT)
                .setSessionCommand(NEXT_COMMAND)
                .setDisplayName(getString(R.string.player_next))
                .setSlots(CommandButton.SLOT_OVERFLOW)
                .build(),
        )
    }

    private fun Player.seekBy(deltaMs: Long) {
        val targetPosition = currentPosition + deltaMs
        val clampedPosition =
            if (duration == C.TIME_UNSET) {
                targetPosition.coerceAtLeast(0L)
            } else {
                targetPosition.coerceIn(0L, duration)
            }
        seekTo(clampedPosition)
    }

    private fun Player.cyclePlaybackSpeed() {
        setPlaybackSpeed(PlaybackSpeeds.nextAfter(playbackParameters.speed))
    }

    private companion object {
        const val ACTION_SKIP_BACK = "ink.duo3.tuned.player.media3.SKIP_BACK_15"
        const val ACTION_SKIP_FORWARD = "ink.duo3.tuned.player.media3.SKIP_FORWARD_30"
        const val ACTION_CYCLE_SPEED = "ink.duo3.tuned.player.media3.CYCLE_SPEED"
        const val ACTION_NEXT = "ink.duo3.tuned.player.media3.NEXT"
        const val SKIP_BACK_MS = -15_000L
        const val SKIP_FORWARD_MS = 30_000L
        const val INACTIVE_NOTIFICATION_TIMEOUT_MS = 300_000L

        val SKIP_BACK_COMMAND = SessionCommand(ACTION_SKIP_BACK, Bundle.EMPTY)
        val SKIP_FORWARD_COMMAND = SessionCommand(ACTION_SKIP_FORWARD, Bundle.EMPTY)
        val CYCLE_SPEED_COMMAND = SessionCommand(ACTION_CYCLE_SPEED, Bundle.EMPTY)
        val NEXT_COMMAND = SessionCommand(ACTION_NEXT, Bundle.EMPTY)
        val SPEED_ICONS =
            mapOf(
                0.8f to R.drawable.ic_speed_0_8x,
                1f to R.drawable.ic_speed_1x,
                1.3f to R.drawable.ic_speed_1_3x,
                1.5f to R.drawable.ic_speed_1_5x,
                1.8f to R.drawable.ic_speed_1_8x,
                2f to R.drawable.ic_speed_2x,
            )
    }
}
