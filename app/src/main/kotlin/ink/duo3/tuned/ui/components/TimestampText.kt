package ink.duo3.tuned.ui.components

import android.content.res.Resources
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import ink.duo3.tuned.R
import ink.duo3.tuned.core.TimestampParts
import ink.duo3.tuned.core.classifyTimestamp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats [epochMillis] as a localized timestamp, per [LocalTimeFormatOptions]. Relative mode reads
 * "just now / N minutes ago / N hours ago" within the day, then named days / dates; precise mode shows
 * the clock on the same day and a date otherwise. The time of day is appended only when [showTime] is
 * set (the podcast detail page) — cards leave it off. Words come from string resources and the date
 * from a locale-aware skeleton, so the display follows the current locale; locales without a word for
 * the day-before/after (e.g. English) fall back to an absolute date.
 */
@Composable
fun rememberRelativeTimestamp(
    epochMillis: Long,
    showTime: Boolean = false,
): String {
    val options = LocalTimeFormatOptions.current
    val resources = LocalContext.current.resources
    val locale = LocalConfiguration.current.locales[0]
    return remember(epochMillis, options, locale, showTime) {
        renderTimestamp(resources, locale, classifyTimestamp(epochMillis, options = options), showTime)
    }
}

private fun renderTimestamp(
    resources: Resources,
    locale: Locale,
    parts: TimestampParts,
    showTime: Boolean,
): String =
    when (parts) {
        TimestampParts.JustNow -> resources.getString(R.string.time_just_now)
        is TimestampParts.RelativeMinutes ->
            resources.getQuantityString(
                if (parts.future) R.plurals.time_minutes_later else R.plurals.time_minutes_ago,
                parts.value,
                parts.value,
            )
        is TimestampParts.RelativeHours ->
            resources.getQuantityString(
                if (parts.future) R.plurals.time_hours_later else R.plurals.time_hours_ago,
                parts.value,
                parts.value,
            )
        is TimestampParts.ClockOnly -> parts.clock
        is TimestampParts.NamedDay -> renderNamedDay(resources, locale, parts, showTime)
        is TimestampParts.AbsoluteDate ->
            withClock(formatDate(locale, parts.date, parts.sameYear), parts.clock, showTime)
    }

private fun renderNamedDay(
    resources: Resources,
    locale: Locale,
    day: TimestampParts.NamedDay,
    showTime: Boolean,
): String {
    val isFarDay = day.offset == 2 || day.offset == -2
    val noFarDayWord = isFarDay && !resources.getBoolean(R.bool.time_named_day_before_after)
    val base =
        if (noFarDayWord) {
            formatDate(locale, day.date, day.sameYear)
        } else {
            resources.getString(namedDayWord(day.offset))
        }
    return withClock(base, day.clock, showTime)
}

private fun namedDayWord(offset: Int): Int =
    when (offset) {
        2 -> R.string.time_day_after_tomorrow
        1 -> R.string.time_tomorrow
        -1 -> R.string.time_yesterday
        else -> R.string.time_day_before_yesterday
    }

private fun withClock(
    text: String,
    clock: String,
    showTime: Boolean,
): String = if (showTime) "$text $clock" else text

private fun formatDate(
    locale: Locale,
    date: LocalDate,
    sameYear: Boolean,
): String {
    val pattern = DateFormat.getBestDateTimePattern(locale, if (sameYear) "MMMd" else "yMMMd")
    return DateTimeFormatter.ofPattern(pattern, locale).format(date)
}
