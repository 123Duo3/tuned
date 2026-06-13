package ink.duo3.tuned.domain.player

import java.util.Locale
import kotlin.math.abs

object PlaybackSpeeds {
    const val EPSILON = 0.01f

    val presets: List<Float> = listOf(0.8f, 1f, 1.3f, 1.5f, 1.8f, 2f)

    fun nextAfter(current: Float): Float = presets.firstOrNull { it > current + EPSILON } ?: presets.first()

    fun closestPreset(speed: Float): Float = presets.minBy { abs(it - speed) }

    fun label(speed: Float): String {
        val preset = closestPreset(speed)
        return if (abs(preset - speed) <= EPSILON) {
            labels.getValue(preset)
        } else {
            String.format(Locale.US, "%.1f", speed)
        }
    }

    private val labels: Map<Float, String> =
        mapOf(
            0.8f to "0.8",
            1f to "1",
            1.3f to "1.3",
            1.5f to "1.5",
            1.8f to "1.8",
            2f to "2",
        )
}
