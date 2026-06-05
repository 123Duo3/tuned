package ink.duo3.tuned.data.repository

import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.data.network.ChaptersApi
import ink.duo3.tuned.data.network.Id3TestFixtures
import ink.duo3.tuned.data.network.Id3TestFixtures.ChapterSpec
import ink.duo3.tuned.data.network.Id3TestFixtures.Image
import ink.duo3.tuned.domain.model.Chapter
import ink.duo3.tuned.domain.model.Episode
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class ChaptersRepositoryImplTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val cacheDir = createTempDirectory("chapters-test").toFile()

    private fun repo(engine: MockEngine) = ChaptersRepositoryImpl(ChaptersApi(HttpClient(engine), json), cacheDir)

    @Test
    fun `prefers podcasting 2_0 json and sorts by start time`() =
        runBlocking {
            val engine = MockEngine { respond(PC20_JSON, HttpStatusCode.OK) }
            val chapters = success(repo(engine).chapters(episode(chaptersUrl = JSON_URL)))

            assertEquals(listOf(0L, 90_000L), chapters.map { it.startTimeMs })
            assertEquals(listOf("Intro", "Topic"), chapters.map { it.title })
        }

    @Test
    fun `falls back to embedded ID3 chapters when no json url`() =
        runBlocking {
            val tag = Id3TestFixtures.tag(ChapterSpec(elementId = "chp0", startMs = 0, title = "Embedded"))
            val engine = MockEngine { respond(tag, HttpStatusCode.OK) }

            val chapters = success(repo(engine).chapters(episode(enclosureUrl = AUDIO_URL)))

            assertEquals(listOf("Embedded"), chapters.map { it.title })
        }

    @Test
    fun `writes embedded chapter art to the cache as a file url`() =
        runBlocking {
            val picture = byteArrayOf(9, 8, 7, 6, 5)
            val tag =
                Id3TestFixtures.tag(
                    ChapterSpec(
                        elementId = "chp0",
                        startMs = 0,
                        title = "Art",
                        image = Image(mimeType = "image/png", data = picture),
                    ),
                )
            val engine = MockEngine { respond(tag, HttpStatusCode.OK) }

            val imageUrl = success(repo(engine).chapters(episode(enclosureUrl = AUDIO_URL))).single().imageUrl

            assertTrue("expected a file url, got $imageUrl", imageUrl!!.startsWith("file://"))
            assertArrayEquals(picture, File(imageUrl.removePrefix("file://")).readBytes())
        }

    @Test
    fun `json wins over embedded chapters when both exist`() =
        runBlocking {
            val tag = Id3TestFixtures.tag(ChapterSpec(elementId = "chp0", startMs = 0, title = "Embedded"))
            val engine =
                MockEngine { request ->
                    if (request.url.fullPath.endsWith(".json")) {
                        respond(PC20_JSON, HttpStatusCode.OK)
                    } else {
                        respond(tag, HttpStatusCode.OK)
                    }
                }

            val chapters = success(repo(engine).chapters(episode(chaptersUrl = JSON_URL, enclosureUrl = AUDIO_URL)))

            assertEquals(listOf("Intro", "Topic"), chapters.map { it.title })
        }

    @Test
    fun `falls back to embedded chapters when the json document is empty`() =
        runBlocking {
            val tag = Id3TestFixtures.tag(ChapterSpec(elementId = "chp0", startMs = 0, title = "Embedded"))
            val engine =
                MockEngine { request ->
                    if (request.url.fullPath.endsWith(".json")) {
                        respond("""{"version":"1.2.0"}""", HttpStatusCode.OK)
                    } else {
                        respond(tag, HttpStatusCode.OK)
                    }
                }

            val chapters = success(repo(engine).chapters(episode(chaptersUrl = JSON_URL, enclosureUrl = AUDIO_URL)))

            assertEquals(listOf("Embedded"), chapters.map { it.title })
        }

    @Test
    fun `caches by episode id so a second call does not re-fetch`() =
        runBlocking {
            var calls = 0
            val engine =
                MockEngine {
                    calls++
                    respond(PC20_JSON, HttpStatusCode.OK)
                }
            val repo = repo(engine)

            repo.chapters(episode(chaptersUrl = JSON_URL))
            val before = calls
            repo.chapters(episode(chaptersUrl = JSON_URL))

            assertEquals(before, calls)
        }

    @Test
    fun `an episode with no chapter sources resolves to empty`() =
        runBlocking {
            val tag = Id3TestFixtures.tag() // ID3 tag with no CHAP frames
            val engine = MockEngine { respond(tag, HttpStatusCode.OK) }

            assertTrue(success(repo(engine).chapters(episode(enclosureUrl = AUDIO_URL))).isEmpty())
        }

    private fun success(outcome: Outcome<List<Chapter>>) = (outcome as Outcome.Success).value

    private fun episode(
        id: String = "e1",
        chaptersUrl: String? = null,
        enclosureUrl: String? = null,
    ) = Episode(
        id = id,
        podcastId = "p1",
        title = "Episode",
        description = null,
        enclosureUrl = enclosureUrl,
        artworkUrl = null,
        publishedAtMs = 0L,
        durationMs = null,
        chaptersUrl = chaptersUrl,
    )

    private companion object {
        const val JSON_URL = "https://chapters.example/ep.json"
        const val AUDIO_URL = "https://cdn.example/ep.mp3"
        val PC20_JSON =
            """
            {
              "version": "1.2.0",
              "chapters": [
                {"startTime": 90, "title": "Topic"},
                {"startTime": 0, "title": "Intro"}
              ]
            }
            """.trimIndent()
    }
}
