package ink.duo3.tuned.player.media3

import ink.duo3.tuned.domain.model.Chapter
import org.junit.Test
import kotlin.test.assertEquals

class ChapterMetadataUpdaterTest {
    @Test
    fun `titled chapter becomes title with episode subtitle and chapter art`() {
        val result = presentation(Chapter(10_000, "A chapter", "https://example.com/chapter.jpg"), 12_000)

        assertEquals("A chapter", result.title)
        assertEquals(EPISODE_TITLE, result.subtitle)
        assertEquals("https://example.com/chapter.jpg", result.artworkUrl)
    }

    @Test
    fun `untitled chapter keeps episode hierarchy but can replace artwork`() {
        val result = presentation(Chapter(10_000, null, "https://example.com/chapter.jpg"), 12_000)

        assertEquals(EPISODE_TITLE, result.title)
        assertEquals(PODCAST_TITLE, result.subtitle)
        assertEquals("https://example.com/chapter.jpg", result.artworkUrl)
    }

    @Test
    fun `position before first chapter uses episode metadata`() {
        val result = presentation(Chapter(10_000, "A chapter", "https://example.com/chapter.jpg"), 5_000)

        assertEquals(EPISODE_TITLE, result.title)
        assertEquals(PODCAST_TITLE, result.subtitle)
        assertEquals(EPISODE_ARTWORK, result.artworkUrl)
    }

    private fun presentation(
        chapter: Chapter,
        positionMs: Long,
    ): ChapterNotificationPresentation =
        chapterNotificationPresentation(
            chapters = listOf(chapter),
            positionMs = positionMs,
            episodeTitle = EPISODE_TITLE,
            podcastTitle = PODCAST_TITLE,
            episodeArtworkUrl = EPISODE_ARTWORK,
        )

    private companion object {
        const val EPISODE_TITLE = "An episode"
        const val PODCAST_TITLE = "A podcast"
        const val EPISODE_ARTWORK = "https://example.com/episode.jpg"
    }
}
