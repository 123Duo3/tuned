package ink.duo3.tuned.data.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/** Covers SAX behavior that differs between Android's Expat reader and the JVM reader. */
@RunWith(AndroidJUnit4::class)
class RssFeedParserAndroidTest {
    private val parser = RssFeedParser()

    @Test
    fun parsesRssWithAndroidSaxReader() {
        val feed =
            """
            <rss version="2.0"><channel><title>Podcast</title>
            <item><guid>episode</guid><enclosure url="https://cdn/episode.mp3" type="audio/mpeg"/></item>
            </channel></rss>
            """.trimIndent().byteInputStream().use(parser::parse)

        assertEquals("Podcast", feed.title)
        assertEquals("https://cdn/episode.mp3", feed.items.single().enclosureUrl)
    }

    @Test
    fun rejectsDoctypeWithAndroidSaxReader() {
        assertThrows(FeedParseException::class.java) {
            """
            <!DOCTYPE rss [<!ENTITY greeting "hello">]>
            <rss version="2.0"><channel><title>&greeting;</title></channel></rss>
            """.trimIndent().byteInputStream().use(parser::parse)
        }
    }
}
