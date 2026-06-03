package ink.duo3.tuned.data.repository

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.data.opml.OpmlParser
import ink.duo3.tuned.domain.model.Episode
import ink.duo3.tuned.domain.model.OpmlImportResult
import ink.duo3.tuned.domain.model.Podcast
import ink.duo3.tuned.domain.model.RecentEpisode
import ink.duo3.tuned.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpmlRepositoryImplTest {
    private fun podcast(
        id: String,
        feedUrl: String,
        title: String?,
    ) = Podcast(id = id, feedUrl = feedUrl, title = title, author = null, description = null, artworkUrl = null)

    @Test
    fun `import subscribes to every feed and counts per-feed failures`() =
        runTest {
            val podcasts = FakePodcastRepository()
            podcasts.subscribeResults["https://example.com/nested.xml"] = Outcome.Failure(AppError.Network())
            val repository = OpmlRepositoryImpl(OpmlParser(), podcasts)
            val content = readFixture("standard.opml")

            val outcome = repository.import(content)

            // standard.opml yields 3 unique feeds; one of them fails to subscribe.
            assertEquals(Outcome.Success(OpmlImportResult(imported = 2, failed = 1)), outcome)
            assertEquals(
                listOf(
                    "https://example.com/flat.xml",
                    "https://example.com/nested.xml",
                    "https://example.com/textonly.xml",
                ),
                podcasts.subscribedUrls,
            )
        }

    @Test
    fun `a malformed document fails the whole import without subscribing`() =
        runTest {
            val podcasts = FakePodcastRepository()
            val repository = OpmlRepositoryImpl(OpmlParser(), podcasts)

            val outcome = repository.import(readFixture("not-opml.xml"))

            assertTrue(outcome is Outcome.Failure && outcome.error is AppError.Parsing)
            assertTrue(podcasts.subscribedUrls.isEmpty())
        }

    @Test
    fun `export serializes the current subscriptions into round-trippable OPML`() =
        runTest {
            val podcasts =
                FakePodcastRepository(
                    subscriptions =
                        listOf(
                            podcast("1", "https://example.com/a.xml", "Show A"),
                            podcast("2", "https://example.com/b.xml", null),
                        ),
                )
            val parser = OpmlParser()
            val repository = OpmlRepositoryImpl(parser, podcasts)

            val outcome = repository.export()

            assertTrue(outcome is Outcome.Success)
            val reparsed = parser.parse((outcome as Outcome.Success).value.byteInputStream())
            assertEquals(2, reparsed.size)
            assertEquals("https://example.com/a.xml", reparsed[0].xmlUrl)
            assertEquals("Show A", reparsed[0].title)
            assertEquals("https://example.com/b.xml", reparsed[1].xmlUrl)
        }

    private fun readFixture(name: String): String =
        javaClass.getResourceAsStream("/opml/$name")?.bufferedReader()?.use { it.readText() }
            ?: error("fixture not found: $name")

    private class FakePodcastRepository(
        private val subscriptions: List<Podcast> = emptyList(),
    ) : PodcastRepository {
        val subscribeResults = mutableMapOf<String, Outcome<String>>()
        val subscribedUrls = mutableListOf<String>()

        override fun observeSubscriptions(): Flow<List<Podcast>> = MutableStateFlow(subscriptions)

        override fun observePodcast(podcastId: String): Flow<Podcast?> = flowOf(null)

        override fun observeEpisodes(podcastId: String): Flow<List<Episode>> = flowOf(emptyList())

        override fun observeEpisode(episodeId: String): Flow<Episode?> = flowOf(null)

        override fun observeRecentEpisodes(limit: Int): Flow<List<RecentEpisode>> = flowOf(emptyList())

        override suspend fun subscribe(feedUrl: String): Outcome<String> {
            subscribedUrls.add(feedUrl)
            return subscribeResults[feedUrl] ?: Outcome.Success(feedUrl)
        }

        override suspend fun refresh(podcastId: String): Outcome<Unit> = Outcome.Success(Unit)
    }
}
