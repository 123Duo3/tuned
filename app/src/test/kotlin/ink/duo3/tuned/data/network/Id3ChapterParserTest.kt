package ink.duo3.tuned.data.network

import ink.duo3.tuned.data.network.Id3TestFixtures.ChapterSpec
import ink.duo3.tuned.data.network.Id3TestFixtures.Image
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Id3ChapterParserTest {
    @Test
    fun `parses chapters sorted by start time with titles`() {
        val tag =
            Id3TestFixtures.tag(
                ChapterSpec(elementId = "chp1", startMs = 90_000, title = "Topic"),
                ChapterSpec(elementId = "chp0", startMs = 0, title = "Intro"),
                ChapterSpec(elementId = "chp2", startMs = 132_500, title = "Outro"),
            )

        val chapters = Id3ChapterParser.parse(tag)

        assertEquals(listOf(0L, 90_000L, 132_500L), chapters.map { it.startTimeMs })
        assertEquals(listOf("Intro", "Topic", "Outro"), chapters.map { it.title })
    }

    @Test
    fun `decodes a UTF-16 title with a byte-order mark`() {
        val tag =
            Id3TestFixtures.tag(
                ChapterSpec(
                    elementId = "chp0",
                    startMs = 0,
                    title = "字谈：TypeChat",
                    titleEncoding = Id3TestFixtures.ENCODING_UTF16_BOM,
                ),
            )

        assertEquals("字谈：TypeChat", Id3ChapterParser.parse(tag).single().title)
    }

    @Test
    fun `extracts the APIC picture bytes and mime type`() {
        val picture = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val tag =
            Id3TestFixtures.tag(
                ChapterSpec(
                    elementId = "chp0",
                    startMs = 0,
                    title = "With Art",
                    image = Image(mimeType = "image/png", data = picture),
                ),
            )

        val chapter = Id3ChapterParser.parse(tag).single()
        assertEquals("image/png", chapter.image?.mimeType)
        assertArrayEquals(picture, chapter.image?.data)
    }

    @Test
    fun `a chapter without a picture has no image`() {
        val tag = Id3TestFixtures.tag(ChapterSpec(elementId = "chp0", startMs = 0, title = "No Art"))
        assertNull(Id3ChapterParser.parse(tag).single().image)
    }

    @Test
    fun `parses ID3v2_4 synchsafe frame sizes`() {
        val tag =
            Id3TestFixtures.tag(
                major = 4,
                chapters =
                    arrayOf(
                        ChapterSpec(elementId = "chp0", startMs = 0, title = "Intro"),
                        ChapterSpec(elementId = "chp1", startMs = 5_000, title = "Next"),
                    ),
            )

        assertEquals(listOf("Intro", "Next"), Id3ChapterParser.parse(tag).map { it.title })
    }

    @Test
    fun `non-id3 bytes yield no chapters`() {
        assertTrue(Id3ChapterParser.parse(byteArrayOf(0x4F, 0x67, 0x67, 0x53, 1, 2, 3)).isEmpty())
        assertTrue(Id3ChapterParser.parse(ByteArray(4)).isEmpty())
    }

    @Test
    fun `a tag with no chapter frames yields no chapters`() {
        assertTrue(Id3ChapterParser.parse(Id3TestFixtures.tag()).isEmpty())
    }
}
