package ink.duo3.tuned.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FeedIdentityTest {
    @Test
    fun `podcast id is stable for the same url`() {
        val url = "https://example.com/feed.xml"
        assertEquals(FeedIdentity.podcastId(url), FeedIdentity.podcastId(url))
    }

    @Test
    fun `podcast id ignores trailing slash and host case`() {
        val a = FeedIdentity.podcastId("https://Example.COM/feed/")
        val b = FeedIdentity.podcastId("https://example.com/feed")
        assertEquals(a, b)
    }

    @Test
    fun `podcast id preserves path case and query`() {
        // Paths can be case-sensitive; these are genuinely different feeds.
        assertNotEquals(
            FeedIdentity.podcastId("https://example.com/Feed.xml"),
            FeedIdentity.podcastId("https://example.com/feed.xml"),
        )
        assertNotEquals(
            FeedIdentity.podcastId("https://example.com/feed?format=rss"),
            FeedIdentity.podcastId("https://example.com/feed"),
        )
    }

    @Test
    fun `podcast id keeps query parameter case`() {
        // Query case is significant (e.g. auth tokens); it must not be folded.
        assertNotEquals(
            FeedIdentity.podcastId("https://example.com/feed?token=AbC"),
            FeedIdentity.podcastId("https://example.com/feed?token=abc"),
        )
    }

    @Test
    fun `podcast id keeps query case even without a path`() {
        assertNotEquals(
            FeedIdentity.podcastId("https://example.com?token=AbC"),
            FeedIdentity.podcastId("https://example.com?token=abc"),
        )
    }

    @Test
    fun `different feeds get different podcast ids`() {
        assertNotEquals(
            FeedIdentity.podcastId("https://a.com/feed"),
            FeedIdentity.podcastId("https://b.com/feed"),
        )
    }

    @Test
    fun `episode id prefers guid`() {
        val id =
            FeedIdentity.episodeId(
                podcastId = "p1",
                guid = "guid-123",
                enclosureUrl = "https://cdn.example.com/ep.mp3",
                title = "Episode One",
                publishedAtMs = 1_000L,
            )
        // Changing enclosure/title/date must not move the id when a guid is present.
        val sameGuidDifferentRest =
            FeedIdentity.episodeId(
                podcastId = "p1",
                guid = "guid-123",
                enclosureUrl = "https://other.example.com/moved.mp3",
                title = "Renamed",
                publishedAtMs = 2_000L,
            )
        assertEquals(id, sameGuidDifferentRest)
    }

    @Test
    fun `episode id falls back to enclosure url when guid blank`() {
        val enclosure = "https://cdn.example.com/ep.mp3"
        val viaBlankGuid =
            FeedIdentity.episodeId("p1", guid = "  ", enclosureUrl = enclosure, title = "T", publishedAtMs = 1L)
        val viaNullGuid =
            FeedIdentity.episodeId("p1", guid = null, enclosureUrl = enclosure, title = "T", publishedAtMs = 1L)
        assertEquals(viaBlankGuid, viaNullGuid)

        // Title/date noise must not affect an enclosure-derived id.
        val differentTitleAndDate =
            FeedIdentity.episodeId("p1", guid = null, enclosureUrl = enclosure, title = "X", publishedAtMs = 9L)
        assertEquals(viaNullGuid, differentTitleAndDate)
    }

    @Test
    fun `episode id falls back to title and date when guid and enclosure missing`() {
        val a =
            FeedIdentity.episodeId("p1", guid = null, enclosureUrl = null, title = "Hello", publishedAtMs = 42L)
        val b =
            FeedIdentity.episodeId("p1", guid = null, enclosureUrl = "  ", title = "Hello", publishedAtMs = 42L)
        assertEquals(a, b)

        assertNotEquals(
            a,
            FeedIdentity.episodeId("p1", guid = null, enclosureUrl = null, title = "Hello", publishedAtMs = 43L),
        )
    }

    @Test
    fun `episode id is namespaced by podcast`() {
        assertNotEquals(
            FeedIdentity.episodeId("p1", guid = "g", enclosureUrl = null, title = null, publishedAtMs = null),
            FeedIdentity.episodeId("p2", guid = "g", enclosureUrl = null, title = null, publishedAtMs = null),
        )
    }

    @Test
    fun `episode id is null when no identity signal exists`() {
        // guid, enclosure, title and date all absent: an unidentifiable item. The
        // import pipeline must skip + log these, not collapse them onto one id.
        assertNull(
            FeedIdentity.episodeId("p1", guid = null, enclosureUrl = "  ", title = "  ", publishedAtMs = null),
        )
    }

    @Test
    fun `episode id is non-null with only a title`() {
        assertNotNull(
            FeedIdentity.episodeId("p1", guid = null, enclosureUrl = null, title = "Title", publishedAtMs = null),
        )
    }

    @Test
    fun `episode id is non-null with only a date`() {
        assertNotNull(
            FeedIdentity.episodeId("p1", guid = null, enclosureUrl = null, title = null, publishedAtMs = 123L),
        )
    }
}
