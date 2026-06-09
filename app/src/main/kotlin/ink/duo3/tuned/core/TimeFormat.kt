package ink.duo3.tuned.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.absoluteValue

/** Whether a timestamp reads relatively ("today", "3 hours ago") or precisely (a date / clock). */
enum class TimeDisplayMode { RELATIVE, PRECISE }

data class TimeFormatOptions(
    val mode: TimeDisplayMode = TimeDisplayMode.RELATIVE,
)

/**
 * A locale-independent classification of a timestamp for display — the UI layer turns it into a
 * localized string (see `rememberRelativeTimestamp`), and decides whether to append [clock] (only the
 * podcast detail page does; cards don't, except a precise same-day stamp which is [ClockOnly]). Kept
 * pure-Kotlin so the day/delta logic stays unit-testable.
 */
sealed interface TimestampParts {
    /** Relative mode, same day, under a minute. */
    data object JustNow : TimestampParts

    /** Relative mode, same day: [value] minutes, [future] when after now. */
    data class RelativeMinutes(
        val value: Int,
        val future: Boolean,
    ) : TimestampParts

    /** Relative mode, same day: [value] hours, [future] when after now. */
    data class RelativeHours(
        val value: Int,
        val future: Boolean,
    ) : TimestampParts

    /** Precise mode, same day: just the clock. */
    data class ClockOnly(
        val clock: String,
    ) : TimestampParts

    /**
     * Relative mode, a named calendar day with [offset] in -2..2 (yesterday = -1). [date]/[sameYear]
     * let the renderer fall back to an absolute date for locales without a word for the
     * day-before/after; [clock] is appended only when the caller asks to show the time.
     */
    data class NamedDay(
        val offset: Int,
        val date: LocalDate,
        val sameYear: Boolean,
        val clock: String,
    ) : TimestampParts

    /** An absolute date; the year shows only when [sameYear] is false, [clock] only when asked. */
    data class AbsoluteDate(
        val date: LocalDate,
        val sameYear: Boolean,
        val clock: String,
    ) : TimestampParts
}

/** Classifies [epochMillis] (in [zone]) into display [TimestampParts] per [options]. */
@Suppress("ReturnCount")
fun classifyTimestamp(
    epochMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
    options: TimeFormatOptions = TimeFormatOptions(),
): TimestampParts {
    val target = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zone)
    val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
    val clock = "${target.hour.pad()}:${target.minute.pad()}"
    val sameYear = target.year == now.year
    val date = target.toLocalDate()
    val offset = (date.toEpochDay() - now.toLocalDate().toEpochDay()).toInt()

    if (options.mode == TimeDisplayMode.PRECISE) {
        return if (offset == 0) TimestampParts.ClockOnly(clock) else TimestampParts.AbsoluteDate(date, sameYear, clock)
    }
    if (offset == 0) return relativeDelta(epochMillis - nowMillis)
    if (offset in -2..2) return TimestampParts.NamedDay(offset, date, sameYear, clock)
    return TimestampParts.AbsoluteDate(date, sameYear, clock)
}

private fun relativeDelta(diffMillis: Long): TimestampParts {
    val seconds = diffMillis / MILLIS_PER_SECOND
    val abs = seconds.absoluteValue
    val future = seconds >= 0
    return when {
        abs < SECONDS_PER_MINUTE -> TimestampParts.JustNow
        abs < SECONDS_PER_HOUR -> TimestampParts.RelativeMinutes(maxOf(abs / SECONDS_PER_MINUTE, 1L).toInt(), future)
        else -> TimestampParts.RelativeHours(maxOf(abs / SECONDS_PER_HOUR, 1L).toInt(), future)
    }
}

private fun Int.pad(): String = toString().padStart(2, '0')

private const val MILLIS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3600L
