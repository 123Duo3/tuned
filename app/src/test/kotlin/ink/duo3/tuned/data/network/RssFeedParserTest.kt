package ink.duo3.tuned.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RssFeedParserTest {
    private val parser = RssFeedParser()

    private fun parse(fixture: String) =
        parser.parse(
            javaClass.getResourceAsStream("/feeds/$fixture")
                ?: error("fixture not found: $fixture"),
        )

    private fun cp(codePoint: Int) = String(Character.toChars(codePoint))

    @Test
    fun `parses channel and episodes from a standard feed`() {
        val feed = parse("standard.xml")
        assertEquals("Standard Podcast", feed.title)
        assertEquals("https://example.com/podcast", feed.link)
        assertEquals(2, feed.items.size)

        val first = feed.items[0]
        assertEquals("Episode One", first.title)
        assertEquals("guid-001", first.guid)
        assertEquals("https://cdn.example.com/ep1.mp3", first.enclosureUrl)
        assertEquals((1 * 3600 + 2 * 60 + 3) * 1000L, first.durationMs)
    }

    @Test
    fun `display fields are extracted with itunes and content-encoded winning`() {
        val feed = parse("standard.xml")
        assertEquals("A standard test podcast.", feed.description)
        assertEquals("Standard Author", feed.author)
        // itunes:image (square) wins over the legacy RSS <image><url>.
        assertEquals("https://example.com/itunes-art.jpg", feed.artworkUrl)
        assertEquals("Show notes one", feed.items[0].description)
        // content:encoded is the fuller HTML notes and wins over <description>.
        assertEquals("<p>Full notes two</p>", feed.items[1].description)
    }

    @Test
    fun `display fields fall back to managingEditor and rss image url`() {
        val feed = parse("display-fallbacks.xml")
        assertEquals("A feed exercising the secondary display fields.", feed.description)
        assertEquals("editor@example.com (Editor Name)", feed.author)
        assertEquals("https://example.com/rss-art.jpg", feed.artworkUrl)
        assertEquals("Just a description", feed.items.single().description)
    }

    @Test
    fun `item-level itunes image is its own artwork and absent items stay null`() {
        // Episode One declares its own <itunes:image>; Episode Two does not and must not
        // inherit the channel art at parse time (that fallback is the UI's job).
        val items = parse("standard.xml").items
        assertEquals("https://example.com/ep1-art.jpg", items[0].artworkUrl)
        assertNull(items[1].artworkUrl)
    }

    @Test
    fun `double-escaped character references in titles are decoded`() {
        // Some feeds (e.g. 字谈字畅 #35) ship emoji as "&amp;#x1F399;": an HTML numeric
        // reference whose ampersand was XML-escaped. XML parsing leaves a literal
        // "&#x1F399;" behind, which a plain-text title would otherwise show verbatim.
        val feed = parse("double-escaped-entities.xml")

        val emoji = intArrayOf(0x1F399, 0x1F602, 0x1F913, 0x1F911, 0x1F60C).joinToString("") { cp(it) }
        assertEquals("#35：Kerning Panic·字谈字串（三）$emoji", feed.items[0].title)
        assertEquals("Double Escaped ✨ Show", feed.title)
        // Decimal references and double-escaped named entities decode too.
        assertEquals("Lone ${cp(0x1F600)} smile & dash — end", feed.items[1].title)
        // A normal ampersand (no reference) is left untouched.
        assertEquals("Q&A about C&A & co.", feed.items[2].title)
    }

    @Test
    fun `channel title is not overwritten by image title`() {
        // The <image> block also has <title>/<link>; those must not leak into the channel.
        val feed = parse("standard.xml")
        assertEquals("Standard Podcast", feed.title)
        assertEquals("https://example.com/podcast", feed.link)
    }

    @Test
    fun `numeric itunes duration is read as seconds`() {
        val feed = parse("standard.xml")
        assertEquals(3_600_000L, feed.items[1].durationMs)
    }

    @Test
    fun `mm ss duration is parsed`() {
        val feed = parse("missing-guid.xml")
        assertEquals(5 * 60 * 1000L, feed.items.single().durationMs)
    }

    @Test
    fun `missing guid yields a null guid rather than failing`() {
        val episode = parse("missing-guid.xml").items.single()
        assertNull(episode.guid)
        assertEquals("Guidless Episode", episode.title)
        assertEquals("https://cdn.example.com/noguid.mp3", episode.enclosureUrl)
    }

    @Test
    fun `malformed date and duration become null without dropping the item`() {
        val feed = parse("malformed-items.xml")
        assertEquals(3, feed.items.size)

        val bad = feed.items[1]
        assertEquals("guid-bad", bad.guid)
        assertNull(bad.publishedAtMs)
        assertNull(bad.durationMs)
    }

    @Test
    fun `item with only a guid has all other fields null`() {
        val empty = parse("malformed-items.xml").items[2]
        assertEquals("guid-empty", empty.guid)
        assertNull(empty.title)
        assertNull(empty.enclosureUrl)
        assertNull(empty.publishedAtMs)
        assertNull(empty.durationMs)
    }

    @Test
    fun `duplicate guids are preserved as separate items`() {
        // The parser does no dedup; collapsing duplicates is the import mapper's job.
        val feed = parse("duplicates.xml")
        assertEquals(2, feed.items.size)
        assertTrue(feed.items.all { it.guid == "shared-guid" })
        assertEquals("https://cdn.example.com/first.mp3", feed.items[0].enclosureUrl)
        assertEquals("https://cdn.example.com/second.mp3", feed.items[1].enclosureUrl)
    }

    @Test
    fun `not well-formed xml throws FeedParseException`() {
        assertThrows(FeedParseException::class.java) {
            parse("not-well-formed.xml")
        }
    }

    @Test
    fun `doctype is rejected before entity expansion`() {
        assertThrows(FeedParseException::class.java) {
            """
            <!DOCTYPE rss [<!ENTITY greeting "hello">]>
            <rss version="2.0"><channel><title>&greeting;</title></channel></rss>
            """.trimIndent().byteInputStream().use(parser::parse)
        }
    }

    @Test
    fun `well-formed non-rss is rejected rather than imported empty`() {
        // A mistyped URL returning Atom/OPML/HTML must not become a silent empty feed.
        assertThrows(FeedParseException::class.java) {
            parse("atom.xml")
        }
    }

    @Test
    fun `rfc822 two-digit year is parsed`() {
        val items = parse("rfc822-dates.xml").items
        assertEquals(Instant.parse("2025-06-10T09:00:00Z").toEpochMilli(), items[0].publishedAtMs)
    }

    @Test
    fun `rfc822 named us timezone is parsed`() {
        val items = parse("rfc822-dates.xml").items
        assertEquals(Instant.parse("2025-06-10T14:00:00Z").toEpochMilli(), items[1].publishedAtMs)
    }

    @Test
    fun `rfc822 without weekday or seconds is parsed`() {
        val items = parse("rfc822-dates.xml").items
        assertEquals(Instant.parse("2025-06-10T09:00:00Z").toEpochMilli(), items[2].publishedAtMs)
    }

    @Test
    fun `first audio enclosure is chosen over a non-audio one and mime is case-insensitive`() {
        // Image first, then "Audio/MPEG" (mixed case) — the first audio still wins.
        val episode = parse("multi-enclosure.xml").items.single()
        assertEquals("https://cdn.example.com/audio.mp3", episode.enclosureUrl)
    }

    @Test
    fun `an explicitly non-audio enclosure is not treated as playable`() {
        // An item whose only enclosure is image/jpeg must not yield a playable URL,
        // or the mapper would surface a JPEG as a "playable" episode.
        val episode = parse("image-only-enclosure.xml").items.single()
        assertNull(episode.enclosureUrl)
    }

    @Test
    fun `itunes new-feed-url is extracted and absent feeds yield null`() {
        assertNull(parse("standard.xml").newFeedUrl)
        val moved =
            """
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"><channel>
            <title>Moved</title>
            <itunes:new-feed-url>https://example.com/canonical/feed.xml</itunes:new-feed-url>
            </channel></rss>
            """.trimIndent().byteInputStream().use(parser::parse)
        assertEquals("https://example.com/canonical/feed.xml", moved.newFeedUrl)
    }

    @Test
    fun `rss root without a channel is rejected`() {
        assertThrows(FeedParseException::class.java) {
            parse("rss-no-channel.xml")
        }
    }

    @Test
    fun `rss nested inside other xml is rejected`() {
        // A stray <rss> deeper in the tree must not pass as a feed.
        assertThrows(FeedParseException::class.java) {
            parse("nested-rss.xml")
        }
    }
}
