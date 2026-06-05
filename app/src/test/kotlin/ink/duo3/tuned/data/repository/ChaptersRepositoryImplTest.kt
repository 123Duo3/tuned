package ink.duo3.tuned.data.repository

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.data.network.ChaptersApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChaptersRepositoryImplTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun repo(engine: MockEngine) = ChaptersRepositoryImpl(ChaptersApi(HttpClient(engine), json))

    @Test
    fun `maps json chapters into sorted domain models with seconds to millis`() =
        runBlocking {
            val engine = MockEngine { respond(CHAPTERS, HttpStatusCode.OK) }
            val result = repo(engine).chapters("https://chapters.example/ep.json")

            val chapters = (result as Outcome.Success).value
            assertEquals(listOf(0L, 90_000L, 132_500L), chapters.map { it.startTimeMs })
            assertEquals(listOf("Intro", "Topic", "Outro"), chapters.map { it.title })
            assertEquals("https://img.example/intro.jpg", chapters.first().imageUrl)
            assertEquals("https://link.example/topic", chapters[1].url)
        }

    @Test
    fun `drops entries with no start time and toc-false image cues`() =
        runBlocking {
            val engine = MockEngine { respond(NOISY_CHAPTERS, HttpStatusCode.OK) }
            val result = repo(engine).chapters("https://chapters.example/noisy.json")

            assertEquals(listOf("Keep"), (result as Outcome.Success).value.map { it.title })
        }

    @Test
    fun `a document with no chapters is an empty success not a failure`() =
        runBlocking {
            val engine = MockEngine { respond("""{"version":"1.2.0"}""", HttpStatusCode.OK) }
            val result = repo(engine).chapters("https://chapters.example/empty.json")

            assertEquals(emptyList<String>(), (result as Outcome.Success).value.map { it.title })
        }

    @Test
    fun `caches by url so a second call does not re-fetch`() =
        runBlocking {
            var calls = 0
            val engine =
                MockEngine {
                    calls++
                    respond(CHAPTERS, HttpStatusCode.OK)
                }
            val repo = repo(engine)
            val url = "https://chapters.example/ep.json"

            repo.chapters(url)
            repo.chapters(url)

            assertEquals(1, calls)
        }

    @Test
    fun `maps a non-2xx status to an http error`() =
        runBlocking {
            val engine = MockEngine { respond("nope", HttpStatusCode.NotFound) }
            val result = repo(engine).chapters("https://chapters.example/missing.json")

            assertTrue((result as Outcome.Failure).error is AppError.Http)
            assertEquals(404, (result.error as AppError.Http).code)
        }

    @Test
    fun `maps a malformed body to a parsing error`() =
        runBlocking {
            val engine = MockEngine { respond("not json", HttpStatusCode.OK) }
            val result = repo(engine).chapters("https://chapters.example/garbage.json")

            assertTrue((result as Outcome.Failure).error is AppError.Parsing)
        }

    private companion object {
        val CHAPTERS =
            """
            {
              "version": "1.2.0",
              "chapters": [
                {"startTime": 90, "title": "Topic", "url": "https://link.example/topic"},
                {"startTime": 0, "title": "Intro", "img": "https://img.example/intro.jpg"},
                {"startTime": 132.5, "title": "Outro"}
              ]
            }
            """.trimIndent()

        val NOISY_CHAPTERS =
            """
            {
              "chapters": [
                {"title": "No start time, dropped"},
                {"startTime": 10, "title": "Image cue", "img": "https://img.example/cue.jpg", "toc": false},
                {"startTime": 5, "title": "Keep"}
              ]
            }
            """.trimIndent()
    }
}
