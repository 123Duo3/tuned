package ink.duo3.tuned.player.media3

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
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
        mediaSession = MediaSession.Builder(this, exo).build()
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
}
