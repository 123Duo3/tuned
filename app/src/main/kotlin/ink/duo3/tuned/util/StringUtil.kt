package ink.duo3.tuned.util

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun String.pubDateToLong(): Long {
    return ZonedDateTime
        .parse(this, DateTimeFormatter.RFC_1123_DATE_TIME)
        .toEpochSecond()
        .times(1000)
}

fun String.toEpisodeLength(): String {
    val time = this.split(":").map { it.toInt() }
    return when (time.size) {
        1 -> {
            "${time[0]} seconds"
        }

        2 -> {
            "${time[0]} minutes"
        }

        3 -> {
            "${time[0]} hours ${time[1]} minutes"
        }

        else -> {
            ""
        }
    }
}