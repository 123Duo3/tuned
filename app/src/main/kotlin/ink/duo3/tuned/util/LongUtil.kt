package ink.duo3.tuned.util

fun Long.toTimeAgo(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val months = days / 30
    val years = months / 12
    when {
        years > 0 -> {
            return "$years years ago"
        }

        months > 0 -> {
            return "$months months ago"
        }

        days > 0 -> {
            return "$days days ago"
        }

        hours > 0 -> {
            return "$hours hours ago"
        }

        minutes > 0 -> {
            return "$minutes minutes ago"
        }

        else -> {
            return "$seconds seconds ago"
        }
    }
}