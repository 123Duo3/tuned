package ink.duo3.tuned.data.network

import ink.duo3.tuned.data.model.ParsedEpisode
import ink.duo3.tuned.data.model.ParsedFeed
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.util.Locale
import javax.xml.parsers.SAXParserFactory

/** Thrown when the document is not well-formed XML or cannot be read. */
class FeedParseException(
    message: String,
    cause: Throwable?,
) : Exception(message, cause)

/**
 * Streaming RSS 2.0 parser. SAX is used over [android.util.Xml] so the same code
 * runs in JVM unit tests and on device. Extraction only — identity, dedup and
 * redirect handling are the import mapper's job.
 */
class RssFeedParser {
    fun parse(input: InputStream): ParsedFeed {
        val handler = RssHandler()
        try {
            newSecureFactory().newSAXParser().parse(input, handler)
        } catch (e: SAXException) {
            throw FeedParseException("Malformed RSS feed", e)
        } catch (e: IOException) {
            throw FeedParseException("Failed to read RSS feed", e)
        }
        return buildFeed(handler)
    }

    private fun buildFeed(handler: RssHandler): ParsedFeed {
        // Well-formed but non-RSS (Atom, OPML, an HTML error page) would otherwise
        // import as a silent empty subscription; reject it instead.
        if (!handler.sawRss) {
            throw FeedParseException("Not an RSS feed (missing <rss> root)", null)
        }
        return ParsedFeed(
            title = handler.channelTitle?.trim()?.ifBlank { null },
            link = handler.channelLink?.trim()?.ifBlank { null },
            items = handler.items,
        )
    }

    private fun newSecureFactory(): SAXParserFactory =
        SAXParserFactory.newInstance().apply {
            // Harden against XXE / entity-expansion attacks from untrusted feeds.
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isNamespaceAware = false
        }
}

private class RssHandler : DefaultHandler() {
    var sawRss = false
    var channelTitle: String? = null
    var channelLink: String? = null
    val items = mutableListOf<ParsedEpisode>()

    private val text = StringBuilder()
    private var inItem = false
    private var inImage = false

    private var itemGuid: String? = null
    private var itemTitle: String? = null
    private var itemEnclosureUrl: String? = null
    private var itemEnclosureIsAudio = false
    private var itemPubDate: String? = null
    private var itemDuration: String? = null

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
    ) {
        text.setLength(0)
        when (qName) {
            "rss" -> sawRss = true
            "item" -> {
                inItem = true
                itemGuid = null
                itemTitle = null
                itemEnclosureUrl = null
                itemEnclosureIsAudio = false
                itemPubDate = null
                itemDuration = null
            }
            "image" -> inImage = true
            "enclosure" -> if (inItem) selectEnclosure(attributes)
        }
    }

    // RSS items occasionally carry multiple enclosures. Keep the first audio one
    // (upgrading from a non-audio placeholder) so the mapper gets a playable URL.
    private fun selectEnclosure(attributes: Attributes?) {
        val url = attributes?.getValue("url")?.trim().orEmpty()
        if (url.isEmpty()) return
        val isAudio = attributes?.getValue("type")?.startsWith("audio") == true
        if (itemEnclosureUrl == null || (isAudio && !itemEnclosureIsAudio)) {
            itemEnclosureUrl = url
            itemEnclosureIsAudio = isAudio
        }
    }

    override fun characters(
        ch: CharArray,
        start: Int,
        length: Int,
    ) {
        text.appendRange(ch, start, start + length)
    }

    override fun endElement(
        uri: String?,
        localName: String?,
        qName: String?,
    ) {
        val value = text.toString()
        when (qName) {
            "item" -> finishItem()
            "image" -> inImage = false
            else -> if (inItem) collectItemField(qName, value) else collectChannelField(qName, value)
        }
        text.setLength(0)
    }

    private fun finishItem() {
        items.add(
            ParsedEpisode(
                guid = itemGuid?.trim()?.ifBlank { null },
                title = itemTitle?.trim()?.ifBlank { null },
                enclosureUrl = itemEnclosureUrl?.trim()?.ifBlank { null },
                publishedAtMs = parseRfc822(itemPubDate),
                durationMs = parseDuration(itemDuration),
            ),
        )
        inItem = false
    }

    private fun collectItemField(
        qName: String?,
        value: String,
    ) {
        when (qName) {
            "title" -> itemTitle = value
            "guid" -> itemGuid = value
            "pubDate" -> itemPubDate = value
            "itunes:duration" -> itemDuration = value
        }
    }

    private fun collectChannelField(
        qName: String?,
        value: String,
    ) {
        if (inImage) return
        when (qName) {
            "title" -> channelTitle = value
            "link" -> channelLink = value
        }
    }
}

// RSS 2.0 dates are RFC 822: optional weekday, 1-2 digit day, 2- or 4-digit year,
// optional seconds, and a numeric offset or an obsolete zone abbreviation (EST, …).
private val RFC822_ZONE_OFFSETS =
    mapOf(
        "UT" to "+0000",
        "GMT" to "+0000",
        "Z" to "+0000",
        "EST" to "-0500",
        "EDT" to "-0400",
        "CST" to "-0600",
        "CDT" to "-0500",
        "MST" to "-0700",
        "MDT" to "-0600",
        "PST" to "-0800",
        "PDT" to "-0700",
    )

private val RFC822_FORMATTER =
    DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .optionalStart()
        .appendPattern("EEE")
        .appendLiteral(", ")
        .optionalEnd()
        .appendPattern("d MMM ")
        .appendValueReduced(ChronoField.YEAR, 2, 4, 1970)
        .appendLiteral(' ')
        .appendPattern("HH:mm")
        .optionalStart()
        .appendLiteral(':')
        .appendPattern("ss")
        .optionalEnd()
        .appendLiteral(' ')
        .appendOffset("+HHMM", "+0000")
        .toFormatter(Locale.ENGLISH)

private fun parseRfc822(raw: String?): Long? {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return null
    return runCatching {
        RFC822_FORMATTER.parse(normalizeZone(value), Instant::from).toEpochMilli()
    }.getOrElse {
        if (it is DateTimeParseException) null else throw it
    }
}

private fun normalizeZone(value: String): String {
    val cut = value.lastIndexOf(' ')
    val offset =
        if (cut < 0) null else RFC822_ZONE_OFFSETS[value.substring(cut + 1).uppercase(Locale.ENGLISH)]
    return if (offset == null) value else value.substring(0, cut + 1) + offset
}

private fun parseDuration(raw: String?): Long? {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return null
    return runCatching {
        val seconds =
            if (value.contains(':')) {
                value.split(':').fold(0L) { acc, part -> acc * 60 + part.trim().toLong() }
            } else {
                value.toLong()
            }
        seconds * 1000
    }.getOrNull()
}
