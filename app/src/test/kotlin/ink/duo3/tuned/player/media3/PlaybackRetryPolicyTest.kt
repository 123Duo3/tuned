package ink.duo3.tuned.player.media3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackRetryPolicyTest {
    @Test
    fun `backoff grows exponentially from the base delay`() {
        val policy = PlaybackRetryPolicy(maxAttempts = 5, baseDelayMs = 1_000L, maxDelayMs = 60_000L)

        assertEquals(1_000L, policy.nextBackoffMs())
        assertEquals(2_000L, policy.nextBackoffMs())
        assertEquals(4_000L, policy.nextBackoffMs())
        assertEquals(8_000L, policy.nextBackoffMs())
    }

    @Test
    fun `backoff is capped at the max delay`() {
        val policy = PlaybackRetryPolicy(maxAttempts = 10, baseDelayMs = 1_000L, maxDelayMs = 4_000L)

        assertEquals(1_000L, policy.nextBackoffMs())
        assertEquals(2_000L, policy.nextBackoffMs())
        assertEquals(4_000L, policy.nextBackoffMs())
        assertEquals(4_000L, policy.nextBackoffMs())
    }

    @Test
    fun `the budget is exhausted after the max attempts`() {
        val policy = PlaybackRetryPolicy(maxAttempts = 2, baseDelayMs = 1_000L, maxDelayMs = 60_000L)

        assertEquals(1_000L, policy.nextBackoffMs())
        assertEquals(2_000L, policy.nextBackoffMs())
        assertNull(policy.nextBackoffMs())
        assertNull(policy.nextBackoffMs())
    }

    @Test
    fun `reset restores the full budget after recovery`() {
        val policy = PlaybackRetryPolicy(maxAttempts = 2, baseDelayMs = 1_000L, maxDelayMs = 60_000L)

        policy.nextBackoffMs()
        policy.nextBackoffMs()
        assertNull(policy.nextBackoffMs())

        policy.reset()

        assertEquals(1_000L, policy.nextBackoffMs())
        assertEquals(2_000L, policy.nextBackoffMs())
        assertNull(policy.nextBackoffMs())
    }
}
