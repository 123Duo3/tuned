package ink.duo3.tuned.data.local

import ink.duo3.tuned.data.model.ParsedEpisode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeMapperTest {
    private val podcastId = "p1"

    private fun episode(
        guid: String? = null,
        title: String? = null,
        enclosureUrl: String? = null,
        publishedAtMs: Long? = null,
        durationMs: Long? = null,
    ) = ParsedEpisode(
        guid = guid,
        title = title,
        description = null,
        enclosureUrl = enclosureUrl,
        artworkUrl = null,
        publishedAtMs = publishedAtMs,
        durationMs = durationMs,
    )

    @Test
    fun `maps a complete item to an entity with a FeedIdentity id`() {
        val mapping =
            EpisodeMapper.map(
                podcastId,
                listOf(episode("g1", "Title", "https://cdn/a.mp3", 1_000L, 60_000L)),
            )

        assertEquals(0, mapping.skipped)
        val entity = mapping.episodes.single()
        assertEquals(
            FeedIdentity.episodeId(podcastId, "g1", "https://cdn/a.mp3", "Title", 1_000L),
            entity.id,
        )
        assertEquals(podcastId, entity.podcastId)
        assertEquals("g1", entity.guid)
        assertEquals("https://cdn/a.mp3", entity.enclosureUrl)
        assertEquals(1_000L, entity.publishedAt)
        assertEquals(60_000L, entity.durationMs)
    }

    @Test
    fun `title and description are carried onto the entity`() {
        val item =
            ParsedEpisode(
                guid = "g1",
                title = "Episode Title",
                description = "<p>notes</p>",
                enclosureUrl = "https://cdn/a.mp3",
                artworkUrl = "https://cdn/ep.jpg",
                publishedAtMs = 1_000L,
                durationMs = null,
            )
        val entity = EpisodeMapper.map(podcastId, listOf(item)).episodes.single()

        assertEquals("Episode Title", entity.title)
        assertEquals("<p>notes</p>", entity.description)
        assertEquals("https://cdn/ep.jpg", entity.artworkUrl)
    }

    @Test
    fun `an item with no audio is kept as a null-enclosure entity`() {
        // A text-only announcement (no enclosure) still has identity via its guid, so
        // it must surface in the list rather than silently vanish.
        val mapping = EpisodeMapper.map(podcastId, listOf(episode(guid = "g1", title = "Announcement")))

        assertEquals(0, mapping.skipped)
        val entity = mapping.episodes.single()
        assertEquals("Announcement", entity.title)
        assertNull(entity.enclosureUrl)
    }

    @Test
    fun `an item with no identity signal at all is dropped`() {
        // No guid, no enclosure, no title+date: nothing stable to key on, so drop it.
        val mapping = EpisodeMapper.map(podcastId, listOf(episode()))

        assertEquals(0, mapping.episodes.size)
        assertEquals(1, mapping.skipped)
    }

    @Test
    fun `missing published date defaults to zero`() {
        val entity =
            EpisodeMapper
                .map(podcastId, listOf(episode("g1", enclosureUrl = "https://cdn/a.mp3")))
                .episodes
                .single()

        assertEquals(0L, entity.publishedAt)
    }

    @Test
    fun `duplicate ids collapse and the dropped copy is counted`() {
        // Same guid -> same id; with no dates to compare, the first seen wins.
        val mapping =
            EpisodeMapper.map(
                podcastId,
                listOf(
                    episode("dup", enclosureUrl = "https://cdn/first.mp3"),
                    episode("dup", enclosureUrl = "https://cdn/second.mp3"),
                ),
            )

        val entity = mapping.episodes.single()
        assertEquals("https://cdn/first.mp3", entity.enclosureUrl)
        assertEquals(1, mapping.skipped)
    }

    @Test
    fun `duplicate keeps the newer item regardless of position`() {
        // Older copy appears first; the newer publishedAt must win (RSS order is not trusted).
        val mapping =
            EpisodeMapper.map(
                podcastId,
                listOf(
                    episode("dup", enclosureUrl = "https://cdn/old.mp3", publishedAtMs = 100L),
                    episode("dup", enclosureUrl = "https://cdn/new.mp3", publishedAtMs = 200L),
                ),
            )

        val entity = mapping.episodes.single()
        assertEquals("https://cdn/new.mp3", entity.enclosureUrl)
        assertEquals(200L, entity.publishedAt)
        assertEquals(1, mapping.skipped)
    }

    @Test
    fun `input order is preserved`() {
        val mapping =
            EpisodeMapper.map(
                podcastId,
                listOf(
                    episode("g1", enclosureUrl = "https://cdn/1.mp3"),
                    episode("g2", enclosureUrl = "https://cdn/2.mp3"),
                    episode("g3", enclosureUrl = "https://cdn/3.mp3"),
                ),
            )

        assertEquals(
            listOf("https://cdn/1.mp3", "https://cdn/2.mp3", "https://cdn/3.mp3"),
            mapping.episodes.map { it.enclosureUrl },
        )
    }

    @Test
    fun `skipped count covers every dropped item while good ones still map`() {
        val mapping =
            EpisodeMapper.map(
                podcastId,
                listOf(
                    episode("g1", enclosureUrl = "https://cdn/a.mp3"),
                    episode(),
                    episode(),
                ),
            )

        assertEquals(1, mapping.episodes.size)
        assertEquals(2, mapping.skipped)
    }

    @Test
    fun `enclosure-derived id is used when guid is absent`() {
        val entity =
            EpisodeMapper
                .map(podcastId, listOf(episode(enclosureUrl = "https://cdn/a.mp3", title = "T")))
                .episodes
                .single()

        assertEquals(
            FeedIdentity.episodeId(podcastId, null, "https://cdn/a.mp3", "T", null),
            entity.id,
        )
    }
}
