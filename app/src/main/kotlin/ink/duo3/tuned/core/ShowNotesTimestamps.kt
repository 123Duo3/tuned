package ink.duo3.tuned.core

/**
 * Finds `mm:ss` / `h:mm:ss` timestamp markers in episode show notes — the most common way
 * podcasts ship chapter cues. Used both to build a chapter list (lowest-priority source,
 * after Podcasting 2.0 and embedded ID3) and to make the timestamps tappable in the rendered
 * notes.
 *
 * Conservative by design:
 * - a timestamp only counts when it sits at the start of its line (preceded by whitespace or
 *   bullet/bracket characters), the way chapter cues are written — so a mid-sentence event time
 *   like "19:00—21:00" in the show notes is ignored;
 * - a marker set is only recognised when at least [MIN_MARKERS] qualifying timestamps remain;
 * - the seconds field is constrained to 00–59, which also rejects ratios like 16:9 and phone
 *   numbers like 1:800.
 */
object ShowNotesTimestamps {
    data class Marker(
        val range: IntRange,
        val atMs: Long,
    )

    // Optional hours, then minutes:seconds. Lookarounds keep a match from starting or ending
    // inside a longer digit/colon run, so 12:34:56 parses as one h:mm:ss rather than two.
    private val TIMESTAMP = Regex("""(?<![\d:])(?:(\d{1,2}):)?(\d{1,3}):([0-5]\d)(?![\d:])""")
    private val BLOCK_TAG = Regex("""(?i)</?(?:p|div|br|li|ul|ol|h[1-6]|tr|section)[^>]*>""")
    private val TAG = Regex("""<[^>]+>""")
    private val NUMERIC_ENTITY = Regex("""&#(x?[0-9A-Fa-f]+);""")
    private const val MIN_MARKERS = 2

    // Characters allowed between the line start and a timestamp (whitespace + common bullets and
    // brackets). Any other character — a letter, digit, or CJK glyph — means the time is embedded
    // in prose rather than introducing a chapter, so it doesn't qualify.
    private const val LEAD_CHARS = " \t\r -–—•*·>[]()｜|"
    private const val SECONDS_PER_HOUR = 3600L
    private const val SECONDS_PER_MINUTE = 60L
    private const val MILLIS_PER_SECOND = 1000L

    /** Markers in [text], in order of appearance; empty unless at least [MIN_MARKERS] are found. */
    fun find(text: CharSequence): List<Marker> {
        val markers =
            TIMESTAMP
                .findAll(text)
                .filter { atSegmentStart(text, it.range.first) }
                .map { match ->
                    val hours = match.groupValues[1].toLongOrNull() ?: 0L
                    val minutes = match.groupValues[2].toLong()
                    val seconds = match.groupValues[3].toLong()
                    val totalSeconds = hours * SECONDS_PER_HOUR + minutes * SECONDS_PER_MINUTE + seconds
                    Marker(range = match.range, atMs = totalSeconds * MILLIS_PER_SECOND)
                }.toList()
        return if (markers.size >= MIN_MARKERS) markers else emptyList()
    }

    // True when only whitespace/bullet characters separate [start] from the previous newline or
    // the start of the text.
    private fun atSegmentStart(
        text: CharSequence,
        start: Int,
    ): Boolean {
        var i = start - 1
        while (i >= 0 && text[i] != '\n') {
            if (text[i] !in LEAD_CHARS) return false
            i--
        }
        return true
    }

    /**
     * Strips HTML to readable text for [find], turning block-level tags into newlines so each
     * note line is preserved (line breaks are how [atSegmentStart] tells a chapter cue from a
     * mid-sentence time), dropping inline tags, and decoding common entities.
     */
    fun stripHtml(html: String): String = decodeEntities(TAG.replace(BLOCK_TAG.replace(html, "\n"), ""))

    private fun decodeEntities(text: String): String {
        if (!text.contains('&')) return text
        val numeric =
            NUMERIC_ENTITY.replace(text) { match ->
                val body = match.groupValues[1]
                val code =
                    if (body.startsWith("x") || body.startsWith("X")) {
                        body.drop(1).toIntOrNull(16)
                    } else {
                        body.toIntOrNull()
                    }
                code?.let { if (Character.isValidCodePoint(it)) String(Character.toChars(it)) else null }
                    ?: match.value
            }
        return NAMED_ENTITIES.entries.fold(numeric) { acc, (name, value) -> acc.replace(name, value) }
    }

    private val NAMED_ENTITIES =
        mapOf(
            "&amp;" to "&",
            "&lt;" to "<",
            "&gt;" to ">",
            "&quot;" to "\"",
            "&#39;" to "'",
            "&apos;" to "'",
            "&nbsp;" to " ",
        )
}
