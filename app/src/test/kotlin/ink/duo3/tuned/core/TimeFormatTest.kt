package ink.duo3.tuned.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class TimeFormatTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = at(2026, 6, 9, 12, 0)

    @Test
    fun `relative mode classifies the nearby days, carrying a padded clock`() {
        assertEquals(named(-1, "23:00"), classify(at(2026, 6, 8, 23, 0)))
        assertEquals(named(-2, "01:00"), classify(at(2026, 6, 7, 1, 0)))
        assertEquals(named(1, "09:00"), classify(at(2026, 6, 10, 9, 0)))
        assertEquals(named(2, "09:00"), classify(at(2026, 6, 11, 9, 0)))
    }

    @Test
    fun `older dates are absolute, with same-year tracked`() {
        assertEquals(absolute(2026, 3, 5, sameYear = true, "09:00"), classify(at(2026, 3, 5, 9, 0)))
        assertEquals(absolute(2025, 12, 31, sameYear = false, "09:00"), classify(at(2025, 12, 31, 9, 0)))
    }

    @Test
    fun `relative today is always a delta, with no six-hour cap`() {
        assertEquals(TimestampParts.JustNow, classify(at(2026, 6, 9, 11, 59, 30)))
        assertEquals(TimestampParts.RelativeMinutes(20, future = false), classify(at(2026, 6, 9, 11, 40)))
        assertEquals(TimestampParts.RelativeHours(3, future = false), classify(at(2026, 6, 9, 9, 0)))
        // No cap — nine hours ago today is still a delta, not a clock.
        assertEquals(TimestampParts.RelativeHours(9, future = false), classify(at(2026, 6, 9, 3, 0)))
    }

    @Test
    fun `precise mode shows a clock today and a date otherwise`() {
        assertEquals(
            TimestampParts.ClockOnly("09:05"),
            classify(at(2026, 6, 9, 9, 5, 7), TimeFormatOptions(TimeDisplayMode.PRECISE)),
        )
        assertEquals(
            absolute(2026, 6, 8, sameYear = true, "22:15"),
            classify(at(2026, 6, 8, 22, 15), TimeFormatOptions(TimeDisplayMode.PRECISE)),
        )
    }

    private fun classify(
        targetMillis: Long,
        options: TimeFormatOptions = TimeFormatOptions(),
    ): TimestampParts = classifyTimestamp(targetMillis, now, zone, options)

    private fun named(
        offset: Int,
        clock: String,
    ): TimestampParts {
        val date = LocalDate.of(2026, 6, 9).plusDays(offset.toLong())
        return TimestampParts.NamedDay(offset, date, sameYear = date.year == 2026, clock = clock)
    }

    private fun absolute(
        year: Int,
        month: Int,
        day: Int,
        sameYear: Boolean,
        clock: String,
    ): TimestampParts = TimestampParts.AbsoluteDate(LocalDate.of(year, month, day), sameYear, clock)

    @Suppress("LongParameterList")
    private fun at(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int = 0,
    ): Long =
        LocalDateTime
            .of(year, month, day, hour, minute, second)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
}
