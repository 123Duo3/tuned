package ink.duo3.tuned.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedDiscoveryTest {
    @Test
    fun `extracts an absolute feed link from an autodiscovery tag`() {
        val html =
            """
            <html><head>
            <link rel="alternate" type="application/rss+xml" href="https://example.com/feed.xml">
            </head></html>
            """.trimIndent()
        assertEquals(
            listOf("https://example.com/feed.xml"),
            FeedDiscovery.feedLinksIn(html, "https://example.com/"),
        )
    }

    @Test
    fun `resolves a relative feed href against the base url`() {
        val html = """<link rel="alternate" type="application/atom+xml" href="/podcast/feed">"""
        assertEquals(
            listOf("https://example.com/podcast/feed"),
            FeedDiscovery.feedLinksIn(html, "https://example.com/blog/post"),
        )
    }

    @Test
    fun `attribute order does not matter`() {
        val html = """<link href="/feed.xml" type="application/rss+xml" rel="alternate">"""
        assertEquals(
            listOf("https://example.com/feed.xml"),
            FeedDiscovery.feedLinksIn(html, "https://example.com"),
        )
    }

    @Test
    fun `ignores stylesheet and icon links`() {
        val html =
            """
            <link rel="stylesheet" href="/style.css">
            <link rel="icon" type="image/png" href="/favicon.png">
            """.trimIndent()
        assertTrue(FeedDiscovery.feedLinksIn(html, "https://example.com").isEmpty())
    }

    @Test
    fun `returns empty when the page has no feed link`() {
        assertTrue(FeedDiscovery.feedLinksIn("<html><body>hi</body></html>", "https://example.com").isEmpty())
    }

    @Test
    fun `guessPaths builds conventional paths under the origin only`() {
        val paths = FeedDiscovery.guessPaths("https://example.com/some/page?q=1")
        assertTrue(paths.contains("https://example.com/feed"))
        assertTrue(paths.contains("https://example.com/rss.xml"))
        assertTrue(paths.all { it.startsWith("https://example.com/") })
    }

    @Test
    fun `guessPaths is empty for a url without a host`() {
        assertTrue(FeedDiscovery.guessPaths("not a url").isEmpty())
    }
}
