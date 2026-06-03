package ink.duo3.tuned.data.opml

import ink.duo3.tuned.data.model.OpmlOutline
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.XMLReader
import org.xml.sax.ext.LexicalHandler
import org.xml.sax.helpers.DefaultHandler
import java.io.IOException
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory

/** Thrown when the document is not well-formed XML, unreadable, or not OPML. */
class OpmlParseException(
    message: String,
    cause: Throwable?,
) : Exception(message, cause)

/**
 * Reads and writes OPML 2.0 subscription lists. SAX is used (matching
 * [ink.duo3.tuned.data.network.RssFeedParser]) so the same code runs in JVM tests
 * and on device, with external entities and DOCTYPEs rejected.
 *
 * Parsing collects every `<outline>` carrying a non-blank `xmlUrl` (feeds nested
 * inside category outlines included), deduplicating by `xmlUrl`. Title preference is
 * `title` then `text`. The root element must be `<opml>`.
 */
class OpmlParser {
    fun parse(input: InputStream): List<OpmlOutline> {
        val handler = OpmlHandler()
        readInto(handler, input)
        if (handler.rootElement != "opml") {
            throw OpmlParseException("Not an OPML document (need <opml> root)", null)
        }
        return handler.outlines
    }

    private fun readInto(
        handler: OpmlHandler,
        input: InputStream,
    ) {
        try {
            newSecureReader(handler).parse(InputSource(input))
        } catch (e: SAXException) {
            throw OpmlParseException("Malformed OPML document", e)
        } catch (e: IOException) {
            throw OpmlParseException("Failed to read OPML document", e)
        }
    }

    fun build(outlines: List<OpmlOutline>): String =
        buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<opml version=\"2.0\">\n")
            append("  <head>\n")
            append("    <title>Tuned subscriptions</title>\n")
            append("  </head>\n")
            append("  <body>\n")
            outlines.forEach { outline ->
                val text = (outline.title ?: outline.xmlUrl).xmlEscape()
                val url = outline.xmlUrl.xmlEscape()
                append(
                    "    <outline type=\"rss\" text=\"$text\" title=\"$text\" xmlUrl=\"$url\"/>\n",
                )
            }
            append("  </body>\n")
            append("</opml>\n")
        }

    private fun newSecureReader(handler: OpmlHandler): XMLReader =
        SAXParserFactory
            .newInstance()
            .apply { isNamespaceAware = false }
            .newSAXParser()
            .xmlReader
            .apply {
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

private class OpmlHandler : DefaultHandler() {
    var rootElement: String? = null
    val outlines = mutableListOf<OpmlOutline>()

    private var depth = 0
    private val seenUrls = mutableSetOf<String>()

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
    ) {
        if (depth == 0) rootElement = qName
        if (qName == "outline") collectOutline(attributes)
        depth++
    }

    private fun collectOutline(attributes: Attributes?) {
        val xmlUrl = attributes?.getValue("xmlUrl")?.trim().orEmpty()
        if (xmlUrl.isEmpty() || !seenUrls.add(xmlUrl)) return
        val title =
            (attributes?.getValue("title") ?: attributes?.getValue("text"))
                ?.trim()
                ?.ifBlank { null }
        outlines.add(OpmlOutline(title = title, xmlUrl = xmlUrl))
    }

    override fun endElement(
        uri: String?,
        localName: String?,
        qName: String?,
    ) {
        depth--
    }
}

private fun String.xmlEscape(): String =
    buildString(length) {
        this@xmlEscape.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(char)
            }
        }
    }
