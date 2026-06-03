package ink.duo3.tuned.data.opml

import ink.duo3.tuned.data.model.OpmlOutline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OpmlParserTest {
    private val parser = OpmlParser()

    private fun parse(fixture: String) =
        parser.parse(
            javaClass.getResourceAsStream("/opml/$fixture")
                ?: error("fixture not found: $fixture"),
        )

    @Test
    fun `collects flat and nested feeds, dedupes by xmlUrl, and titles them`() {
        val outlines = parse("standard.opml")

        assertEquals(
            listOf(
                OpmlOutline("Flat Feed", "https://example.com/flat.xml"),
                OpmlOutline("Nested Feed", "https://example.com/nested.xml"),
                OpmlOutline("Text only fallback", "https://example.com/textonly.xml"),
            ),
            outlines,
        )
    }

    @Test
    fun `skips outlines without an xmlUrl`() {
        val outlines = parse("standard.opml")

        assertTrue(outlines.all { it.xmlUrl.isNotBlank() })
    }

    @Test
    fun `rejects a well-formed non-OPML document`() {
        assertThrows(OpmlParseException::class.java) { parse("not-opml.xml") }
    }

    @Test
    fun `build emits OPML 2 round-trippable by the parser`() {
        val outlines =
            listOf(
                OpmlOutline("Title & <Ampersand>", "https://example.com/a.xml?x=1&y=2"),
                OpmlOutline(null, "https://example.com/b.xml"),
            )

        val document = parser.build(outlines)
        val reparsed = parser.parse(document.byteInputStream())

        assertEquals(
            listOf(
                OpmlOutline("Title & <Ampersand>", "https://example.com/a.xml?x=1&y=2"),
                // A null title falls back to the URL when written, so it survives as text.
                OpmlOutline("https://example.com/b.xml", "https://example.com/b.xml"),
            ),
            reparsed,
        )
    }
}
