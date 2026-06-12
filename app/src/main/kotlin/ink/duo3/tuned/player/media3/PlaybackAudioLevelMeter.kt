package ink.duo3.tuned.player.media3

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Captures real PCM samples from ExoPlayer's audio processor chain and exposes a compact,
 * UI-friendly loudness history for the currently playing item.
 */
@OptIn(UnstableApi::class)
internal object PlaybackAudioLevelMeter : TeeAudioProcessor.AudioBufferSink {
    private val levels = AtomicReference(SILENCE_BARS)
    private val workingBars = FloatArray(BAR_COUNT)

    private var encoding = C.ENCODING_INVALID
    private var samplesPerBucket = DEFAULT_SAMPLES_PER_BUCKET
    private var bucketSumSquares = 0.0
    private var bucketSamples = 0
    private var enabled = false

    fun snapshot(): List<Float> = levels.get()

    @Synchronized
    fun clear() {
        workingBars.fill(0f)
        bucketSumSquares = 0.0
        bucketSamples = 0
        levels.set(SILENCE_BARS)
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        if (this.enabled == enabled) return
        this.enabled = enabled
        clear()
    }

    @Synchronized
    override fun flush(
        sampleRateHz: Int,
        channelCount: Int,
        encoding: Int,
    ) {
        this.encoding = encoding
        samplesPerBucket =
            (sampleRateHz * channelCount * BUCKET_MILLIS / MILLIS_PER_SECOND)
                .coerceAtLeast(MIN_SAMPLES_PER_BUCKET)
        clear()
    }

    @Synchronized
    override fun handleBuffer(buffer: ByteBuffer) {
        if (!enabled) return
        if (encoding != C.ENCODING_PCM_16BIT) return
        val samples = buffer.slice().order(ByteOrder.LITTLE_ENDIAN)
        while (samples.remaining() >= Short.SIZE_BYTES) {
            val sample = samples.short.toDouble() / Short.MAX_VALUE
            bucketSumSquares += sample * sample
            bucketSamples += 1
            if (bucketSamples >= samplesPerBucket) {
                publishBucket()
            }
        }
    }

    private fun publishBucket() {
        val rms = sqrt(bucketSumSquares / bucketSamples.coerceAtLeast(1)).toFloat()
        val level = rmsToLevel(rms)
        workingBars.copyInto(workingBars, destinationOffset = 1, endIndex = BAR_COUNT - 1)
        workingBars[0] = level
        levels.set(workingBars.toList())
        bucketSumSquares = 0.0
        bucketSamples = 0
    }

    private fun rmsToLevel(rms: Float): Float {
        val decibels = DECIBELS_PER_BEL * log10(rms.coerceAtLeast(MIN_RMS))
        return ((decibels - MIN_DECIBELS) / (MAX_DECIBELS - MIN_DECIBELS)).coerceIn(0f, 1f)
    }
}

private val SILENCE_BARS = List(BAR_COUNT) { 0f }
private const val BAR_COUNT = 5
private const val BUCKET_MILLIS = 35
private const val MILLIS_PER_SECOND = 1_000
private const val MIN_SAMPLES_PER_BUCKET = 1
private const val DEFAULT_SAMPLES_PER_BUCKET = 4_096
private const val MIN_RMS = 0.000_1f
private const val MIN_DECIBELS = -45f
private const val MAX_DECIBELS = -8f
private const val DECIBELS_PER_BEL = 20f
