package ink.duo3.tuned.domain.player

private const val COMPLETION_TAIL_TOLERANCE_MS = 15_000L

fun isPlaybackComplete(
    positionMs: Long,
    durationMs: Long?,
    playbackEnded: Boolean = false,
): Boolean =
    playbackEnded ||
        durationMs
            ?.takeIf { it > 0L }
            ?.let { duration ->
                val position = positionMs.coerceAtLeast(0L)
                position >= duration || duration - position <= COMPLETION_TAIL_TOLERANCE_MS
            } == true
