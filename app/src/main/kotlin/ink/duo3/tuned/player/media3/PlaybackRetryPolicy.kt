package ink.duo3.tuned.player.media3

/**
 * Bounded exponential-backoff budget for re-preparing the player after a transient (network)
 * playback error. Pure and media3-free so the retry schedule is unit-testable; the media3 glue
 * ([PlaybackErrorRecovery]) decides which errors are transient and performs the re-prepare.
 *
 * Each [nextBackoffMs] consumes one attempt and returns a longer delay (base, 2×, 4×…, capped),
 * or null once the budget is spent — at which point the failure is left surfaced. [reset] is
 * called when playback recovers, so a later unrelated drop gets a fresh budget.
 */
internal class PlaybackRetryPolicy(
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val baseDelayMs: Long = DEFAULT_BASE_DELAY_MS,
    private val maxDelayMs: Long = DEFAULT_MAX_DELAY_MS,
) {
    private var attempts = 0

    /** Backoff to wait before the next re-prepare, or null once the attempt budget is spent. */
    fun nextBackoffMs(): Long? {
        if (attempts >= maxAttempts) return null
        val delay = (baseDelayMs shl attempts).coerceAtMost(maxDelayMs)
        attempts++
        return delay
    }

    /** Restores the full budget after playback recovers. */
    fun reset() {
        attempts = 0
    }

    private companion object {
        const val DEFAULT_MAX_ATTEMPTS = 5
        const val DEFAULT_BASE_DELAY_MS = 1_000L
        const val DEFAULT_MAX_DELAY_MS = 30_000L
    }
}
