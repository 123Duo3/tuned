package ink.duo3.tuned.data.network

import ink.duo3.tuned.data.model.ParsedEpisode
import ink.duo3.tuned.data.model.ParsedFeed
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.XMLReader
import org.xml.sax.ext.LexicalHandler
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
            newSecureReader(handler).parse(InputSource(input))
        } catch (e: SAXException) {
            throw FeedParseException("Malformed RSS feed", e)
        } catch (e: IOException) {
            throw FeedParseException("Failed to read RSS feed", e)
        }
        return buildFeed(handler)
    }

    private fun buildFeed(handler: RssHandler): ParsedFeed {
        // Well-formed but non-RSS (Atom, OPML, an HTML error page, or XML that merely
        // contains a stray <rss> somewhere) would otherwise import as a silent empty
        // subscription. Require an <rss> root element with a <channel> child.
        if (handler.rootElement != "rss" || !handler.sawChannel) {
            throw FeedParseException("Not an RSS 2.0 feed (need <rss> root with <channel>)", null)
        }
        return ParsedFeed(
            title = handler.channelTitle.decodedTitle(),
            link = handler.channelLink?.trim()?.ifBlank { null },
            description = handler.channelDescription?.trim()?.ifBlank { null },
            author = handler.channelAuthor?.trim()?.ifBlank { null },
            artworkUrl = handler.channelArtworkUrl?.trim()?.ifBlank { null },
            newFeedUrl = handler.channelNewFeedUrl?.trim()?.ifBlank { null },
            items = handler.items,
        )
    }

    private fun newSecureReader(handler: RssHandler): XMLReader =
        SAXParserFactory
            .newInstance()
            .apply { isNamespaceAware = false }
            .newSAXParser()
            .xmlReader
            .apply {
                // Android's Expat reader does not recognize Xerces' disallow-doctype
                // feature. Disable external entities using portable SAX features and
                // reject every DOCTYPE from the lexical handler instead.
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                setProperty("http://xml.org/sax/properties/lexical-handler", RejectDoctypeHandler)
                contentHandler = handler
            }
}

private object RejectDoctypeHandler : LexicalHandler {
    override fun startDTD(
        name: String?,
        publicId: String?,
        systemId: String?,
    ) = throw SAXException("DOCTYPE is not allowed")

    override fun endDTD() = Unit

    override fun startEntity(name: String?) = Unit

    override fun endEntity(name: String?) = Unit

    override fun startCDATA() = Unit

    override fun endCDATA() = Unit

    override fun comment(
        ch: CharArray?,
        start: Int,
        length: Int,
    ) = Unit
}

@Suppress("TooManyFunctions")
private class RssHandler : DefaultHandler() {
    var rootElement: String? = null
    var sawChannel = false
    var channelTitle: String? = null
    var channelLink: String? = null
    var channelDescription: String? = null
    var channelAuthor: String? = null
    var channelArtworkUrl: String? = null
    var channelNewFeedUrl: String? = null
    val items = mutableListOf<ParsedEpisode>()

    private val text = StringBuilder()
    private var depth = 0
    private var inChannel = false
    private var inItem = false
    private var inImage = false

    private var itemGuid: String? = null
    private var itemTitle: String? = null
    private var itemDescription: String? = null
    private var itemEnclosureUrl: String? = null
    private var itemEnclosureIsAudio = false
    private var itemArtworkUrl: String? = null
    private var itemPubDate: String? = null
    private var itemDuration: String? = null
    private var itemChaptersUrl: String? = null

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
    ) {
        text.setLength(0)
        if (depth == 0) rootElement = qName
        when (qName) {
            "channel" ->
                if (depth == 1 && rootElement == "rss") {
                    inChannel = true
                    sawChannel = true
                }
            "item" -> if (inChannel) startItem()
            "image" -> if (inChannel) inImage = true
            else -> startEmptyElement(qName, attributes)
        }
        depth++
    }

    // Empty elements that carry their payload in attributes (no text body): the audio
    // enclosure, item/channel artwork, and the Podcasting 2.0 chapters document URL.
    private fun startEmptyElement(
        qName: String?,
        attributes: Attributes?,
    ) {
        when (qName) {
            "enclosure" -> if (inItem) selectEnclosure(attributes)
            // itunes:image carries the URL in an href attribute. An item-level one is that
            // episode's own art; otherwise it is the channel's.
            "itunes:image" -> if (inItem || inChannel) selectArtwork(attributes)
            // podcast:chapters carries the JSON document URL in a url attribute; only the
            // JSON type gives per-chapter images, so other types are ignored downstream.
            "podcast:chapters" -> if (inItem) selectChaptersUrl(attributes)
        }
    }

    // Item context wins as the episode's own art (first one only — episodes rarely
    // declare more than one); at channel level this is the square iTunes art, preferred
    // over the legacy RSS <image><url> (set in collectImageField).
    private fun selectArtwork(attributes: Attributes?) {
        val href = attributes?.getValue("href")?.trim().orEmpty()
        if (href.isEmpty()) return
        when {
            inItem -> if (itemArtworkUrl == null) itemArtworkUrl = href
            inChannel -> channelArtworkUrl = href
        }
    }

    // Keep the first JSON chapters URL. type is usually "application/json+chapters" but is
    // sometimes omitted; a non-JSON type (e.g. a future format) is ignored so we never point
    // the chapters loader at a document it can't parse.
    private fun selectChaptersUrl(attributes: Attributes?) {
        if (itemChaptersUrl != null) return
        val url = attributes?.getValue("url")?.trim().orEmpty()
        val type = attributes?.getValue("type")?.trim().orEmpty()
        val isJsonType = type.isEmpty() || type.contains("json", ignoreCase = true)
        if (url.isNotEmpty() && isJsonType) itemChaptersUrl = url
    }

    private fun startItem() {
        inItem = true
        itemGuid = null
        itemTitle = null
        itemDescription = null
        itemEnclosureUrl = null
        itemEnclosureIsAudio = false
        itemArtworkUrl = null
        itemPubDate = null
        itemDuration = null
        itemChaptersUrl = null
    }

    // RSS items occasionally carry multiple enclosures. Keep the first audio one,
    // upgrading from a missing-MIME fallback, so the mapper gets a playable URL.
    // An explicit non-audio MIME (cover art, video) is ignored outright — otherwise an
    // image-only item would surface as a bogus "playable" episode pointing at a JPEG.
    private fun selectEnclosure(attributes: Attributes?) {
        val url = attributes?.getValue("url")?.trim().orEmpty()
        if (url.isEmpty()) return
        val type = attributes?.getValue("type")?.trim().orEmpty()
        val isAudio = type.startsWith("audio", ignoreCase = true)
        if (type.isNotEmpty() && !isAudio) return
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
        depth--
        val value = text.toString()
        when (qName) {
            "channel" -> inChannel = false
            "item" -> if (inItem) finishItem()
            "image" -> inImage = false
            else ->
                when {
                    inItem -> collectItemField(qName, value)
                    inImage -> collectImageField(qName, value)
                    inChannel -> collectChannelField(qName, value)
                }
        }
        text.setLength(0)
    }

    // Only the RSS <image><url> child interests us; its <title>/<link> mirror the
    // channel and must not overwrite the real channel fields.
    private fun collectImageField(
        qName: String?,
        value: String,
    ) {
        if (qName == "url" && channelArtworkUrl == null) channelArtworkUrl = value
    }

    private fun finishItem() {
        items.add(
            ParsedEpisode(
                guid = itemGuid?.trim()?.ifBlank { null },
                title = itemTitle.decodedTitle(),
                description = itemDescription?.trim()?.ifBlank { null },
                enclosureUrl = itemEnclosureUrl?.trim()?.ifBlank { null },
                artworkUrl = itemArtworkUrl?.trim()?.ifBlank { null },
                publishedAtMs = parseRfc822(itemPubDate),
                durationMs = parseDuration(itemDuration),
                chaptersUrl = itemChaptersUrl?.trim()?.ifBlank { null },
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
            // content:encoded is the fuller HTML show notes; let it win over <description>.
            "description" -> if (itemDescription == null) itemDescription = value
            "content:encoded" -> itemDescription = value
        }
    }

    private fun collectChannelField(
        qName: String?,
        value: String,
    ) {
        when (qName) {
            "title" -> channelTitle = value
            "link" -> channelLink = value
            "description" -> channelDescription = value
            "itunes:author" -> channelAuthor = value
            // A permanently moved feed advertises its canonical URL here; the importer
            // follows it so a redirect mirror doesn't become the stored identity.
            "itunes:new-feed-url" -> channelNewFeedUrl = value
            // managingEditor is the RSS-standard fallback when itunes:author is absent.
            "managingEditor" -> if (channelAuthor == null) channelAuthor = value
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

// Some feeds double-escape emoji and punctuation: the publisher writes an HTML numeric
// reference such as "&#x1F399;" and then XML-escapes the ampersand, so the bytes on the
// wire are "&amp;#x1F399;". XML parsing resolves only the outer &amp;, leaving the literal
// text "&#x1F399;" in a title that is rendered as plain text (no later HTML pass to finish
// the job). Decode any character reference that survives parsing so titles read correctly.
private val CHARACTER_REFERENCE = Regex("&(#[xX][0-9A-Fa-f]+|#[0-9]+|[A-Za-z][A-Za-z0-9]+);")

private val NAMED_ENTITIES =
    mapOf(
        "amp" to "&",
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "apos" to "'",
        "nbsp" to " ",
        "hellip" to "…",
        "mdash" to "—",
        "ndash" to "–",
        "copy" to "©",
        "reg" to "®",
        "trade" to "™",
        "ldquo" to "“",
        "rdquo" to "”",
        "lsquo" to "‘",
        "rsquo" to "’",
    )

private fun String?.decodedTitle(): String? =
    this
        ?.let(::decodeCharacterReferences)
        ?.trim()
        ?.ifBlank { null }

private fun decodeCharacterReferences(raw: String): String {
    if (!raw.contains('&')) return raw
    return CHARACTER_REFERENCE.replace(raw) { match ->
        val body = match.groupValues[1]
        when {
            body.startsWith("#x", ignoreCase = true) -> codePointToString(body.drop(2), radix = 16) ?: match.value
            body.startsWith("#") -> codePointToString(body.drop(1), radix = 10) ?: match.value
            else -> NAMED_ENTITIES[body] ?: match.value
        }
    }
}

private fun codePointToString(
    digits: String,
    radix: Int,
): String? {
    val cp = digits.toIntOrNull(radix)
    return if (cp == null || cp == 0 || !Character.isValidCodePoint(cp)) null else String(Character.toChars(cp))
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
