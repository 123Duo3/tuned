package ink.duo3.tuned.data.player

import ink.duo3.tuned.data.local.dao.EpisodeDao
import ink.duo3.tuned.data.local.dao.PodcastDao
import ink.duo3.tuned.data.local.dao.ProgressDao
import ink.duo3.tuned.data.local.entity.EpisodeEntity
import ink.duo3.tuned.data.local.entity.PodcastEntity
import ink.duo3.tuned.data.local.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackResumptionSourceImplTest {
    @Test
    fun `builds a playable episode from the most recent progress at its saved position`() =
        runTest {
            val source =
                PlaybackResumptionSourceImpl(
                    progressDao = FakeProgressDao(progress("e1", positionMs = 8_000)),
                    episodeDao = FakeEpisodeDao(episode("e1", "p1", enclosureUrl = "https://audio/e1")),
                    podcastDao = FakePodcastDao(podcast("p1", title = "Pod", artworkUrl = "pod-art")),
                )

            val item = source.lastPlayable()

            assertEquals("e1", item?.episodeId)
            assertEquals("https://audio/e1", item?.streamUrl)
            assertEquals(8_000L, item?.startPositionMs)
            assertEquals("Pod", item?.podcastTitle)
            assertEquals("pod-art", item?.artworkUrl)
        }

    @Test
    fun `episode artwork wins over the podcast artwork fallback`() =
        runTest {
            val source =
                PlaybackResumptionSourceImpl(
                    progressDao = FakeProgressDao(progress("e1", positionMs = 0)),
                    episodeDao =
                        FakeEpisodeDao(
                            episode("e1", "p1", enclosureUrl = "https://audio/e1", artworkUrl = "ep-art"),
                        ),
                    podcastDao = FakePodcastDao(podcast("p1", title = "Pod", artworkUrl = "pod-art")),
                )

            assertEquals("ep-art", source.lastPlayable()?.artworkUrl)
        }

    @Test
    fun `returns null when there is no progress to resume`() =
        runTest {
            val source =
                PlaybackResumptionSourceImpl(FakeProgressDao(null), FakeEpisodeDao(null), FakePodcastDao(null))

            assertNull(source.lastPlayable())
        }

    @Test
    fun `returns null when the recorded episode is gone`() =
        runTest {
            val source =
                PlaybackResumptionSourceImpl(
                    progressDao = FakeProgressDao(progress("e1", positionMs = 1_000)),
                    episodeDao = FakeEpisodeDao(null),
                    podcastDao = FakePodcastDao(null),
                )

            assertNull(source.lastPlayable())
        }

    @Test
    fun `returns null when the episode carries no audio`() =
        runTest {
            val source =
                PlaybackResumptionSourceImpl(
                    progressDao = FakeProgressDao(progress("e1", positionMs = 1_000)),
                    episodeDao = FakeEpisodeDao(episode("e1", "p1", enclosureUrl = null)),
                    podcastDao = FakePodcastDao(podcast("p1", title = "Pod", artworkUrl = "pod-art")),
                )

            assertNull(source.lastPlayable())
        }

    private fun progress(
        episodeId: String,
        positionMs: Long,
    ) = ProgressEntity(episodeId = episodeId, positionMs = positionMs, completed = false, lastPlayedAt = 1L)

    private fun episode(
        id: String,
        podcastId: String,
        enclosureUrl: String?,
        artworkUrl: String? = null,
    ) = EpisodeEntity(
        id = id,
        podcastId = podcastId,
        guid = null,
        enclosureUrl = enclosureUrl,
        publishedAt = 0L,
        durationMs = null,
        title = "Episode $id",
        artworkUrl = artworkUrl,
    )

    private fun podcast(
        id: String,
        title: String,
        artworkUrl: String?,
    ) = PodcastEntity(
        id = id,
        canonicalFeedUrl = "https://feed/$id",
        currentFeedUrl = "https://feed/$id",
        etag = null,
        lastModified = null,
        lastFetchedAt = 0L,
        title = title,
        artworkUrl = artworkUrl,
    )

    private class FakeProgressDao(
        private val recent: ProgressEntity?,
    ) : ProgressDao {
        override suspend fun mostRecent(): ProgressEntity? = recent

        override suspend fun upsert(progress: ProgressEntity) = error("unused")

        override suspend fun findByEpisode(episodeId: String): ProgressEntity? = error("unused")

        override fun observeByEpisode(episodeId: String): Flow<ProgressEntity?> = error("unused")
    }

    private class FakeEpisodeDao(
        private val episode: EpisodeEntity?,
    ) : EpisodeDao {
        override suspend fun findById(id: String): EpisodeEntity? = episode

        override suspend fun upsertAll(episodes: List<EpisodeEntity>) = error("unused")

        override fun observeByPodcast(podcastId: String): Flow<List<EpisodeEntity>> = error("unused")

        override fun observeById(id: String): Flow<EpisodeEntity?> = error("unused")
    }

    private class FakePodcastDao(
        private val podcast: PodcastEntity?,
    ) : PodcastDao {
        override suspend fun findById(id: String): PodcastEntity? = podcast

        override suspend fun upsert(podcast: PodcastEntity) = error("unused")

        override fun observeAll(): Flow<List<PodcastEntity>> = error("unused")

        override fun observeById(id: String): Flow<PodcastEntity?> = error("unused")

        override suspend fun deleteById(id: String) = error("unused")
    }
}
