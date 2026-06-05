package ink.duo3.tuned.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShowNotesTimestampsTest {
    @Test
    fun `finds mm ss and h mm ss markers with millisecond offsets`() {
        val markers = ShowNotesTimestamps.find("00:00 Intro\n12:34 Topic\n1:02:03 Deep dive")

        assertEquals(listOf(0L, 754_000L, 3_723_000L), markers.map { it.atMs })
    }

    @Test
    fun `a single stray timestamp is not treated as a marker set`() {
        assertTrue(ShowNotesTimestamps.find("We talked about the 9:11 attacks at length.").isEmpty())
    }

    @Test
    fun `rejects ratios and non-time colon runs via the seconds constraint`() {
        // 16:9 (seconds field 9 is a single digit) and 1:800 (80 exceeds 59) must not match;
        // with no valid markers left, the set is empty.
        assertTrue(ShowNotesTimestamps.find("Shot in 16:9. Call 1:800 now. Also 4:5 framing.").isEmpty())
    }

    @Test
    fun `does not match inside a longer digit run`() {
        // The ISBN-like run shouldn't yield a 2:34 marker, and there's only prose otherwise.
        assertTrue(ShowNotesTimestamps.find("Order number 998812:3456 shipped.").isEmpty())
    }

    @Test
    fun `ignores mid-line event times in prose`() {
        // A real show-notes case: an event time range embedded in a sentence must not be read
        // as two chapters (19:00 / 21:00).
        val html = "<ul><li>沙龙将于 9 月 13 日 19:00—21:00 在上海举办</li><li>报名截止 9 月 13 日</li></ul>"

        assertTrue(ShowNotesTimestamps.find(ShowNotesTimestamps.stripHtml(html)).isEmpty())
    }

    @Test
    fun `accepts line-leading bullets and brackets before a timestamp`() {
        val markers = ShowNotesTimestamps.find("- 00:00 Intro\n• 12:34 Topic\n(1:02:03) Deep dive")

        assertEquals(listOf(0L, 754_000L, 3_723_000L), markers.map { it.atMs })
    }

    @Test
    fun `strips tags and decodes entities`() {
        val text = ShowNotesTimestamps.stripHtml("<p>00:00 Rock &amp; Roll</p><p>05:30 Caf&#233;</p>")

        val markers = ShowNotesTimestamps.find(text)
        assertEquals(listOf(0L, 330_000L), markers.map { it.atMs })
        assertTrue(text.contains("Rock & Roll"))
        assertTrue(text.contains("Café"))
    }
}
