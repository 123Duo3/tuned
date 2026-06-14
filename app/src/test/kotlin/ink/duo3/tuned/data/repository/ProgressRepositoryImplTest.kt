package ink.duo3.tuned.data.repository

import ink.duo3.tuned.data.local.dao.ProgressDao
import ink.duo3.tuned.data.local.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressRepositoryImplTest {
    @Test
    fun `resume returns the stored position`() =
        runTest {
            val dao = FakeProgressDao(ProgressEntity("e1", positionMs = 5_000, completed = false, lastPlayedAt = 1))
            val repo = ProgressRepositoryImpl(dao) { 0L }

            assertEquals(5_000L, repo.resumePositionMs("e1"))
        }

    @Test
    fun `resume returns zero when there is no stored progress`() =
        runTest {
            val repo = ProgressRepositoryImpl(FakeProgressDao()) { 0L }

            assertEquals(0L, repo.resumePositionMs("missing"))
        }

    @Test
    fun `completed episodes resume from the start`() =
        runTest {
            val dao = FakeProgressDao(ProgressEntity("e1", positionMs = 9_000, completed = true, lastPlayedAt = 1))
            val repo = ProgressRepositoryImpl(dao) { 0L }

            assertEquals(0L, repo.resumePositionMs("e1"))
        }

    @Test
    fun `save upserts the position stamped with the injected clock`() =
        runTest {
            val dao = FakeProgressDao()
            val repo = ProgressRepositoryImpl(dao) { 1_234L }

            repo.save("e1", positionMs = 7_000, completed = false, playbackDurationMs = 60_000L)

            val stored = dao.findByEpisode("e1")
            assertEquals(7_000L, stored?.positionMs)
            assertEquals(1_234L, stored?.lastPlayedAt)
            assertEquals(60_000L, stored?.playbackDurationMs)
        }

    @Test
    fun `save preserves measured duration when the new save has none`() =
        runTest {
            val dao =
                FakeProgressDao(
                    ProgressEntity(
                        episodeId = "e1",
                        positionMs = 7_000L,
                        completed = false,
                        lastPlayedAt = 1L,
                        playbackDurationMs = 60_000L,
                    ),
                )
            val repo = ProgressRepositoryImpl(dao) { 1_234L }

            repo.save("e1", positionMs = 8_000L, completed = false, playbackDurationMs = null)

            val stored = dao.findByEpisode("e1")
            assertEquals(8_000L, stored?.positionMs)
            assertEquals(60_000L, stored?.playbackDurationMs)
        }

    @Test
    fun `save preserves the longer measured duration`() =
        runTest {
            val dao =
                FakeProgressDao(
                    ProgressEntity(
                        episodeId = "e1",
                        positionMs = 7_000L,
                        completed = false,
                        lastPlayedAt = 1L,
                        playbackDurationMs = 60_000L,
                    ),
                )
            val repo = ProgressRepositoryImpl(dao) { 1_234L }

            repo.save("e1", positionMs = 8_000L, completed = false, playbackDurationMs = 50_000L)

            assertEquals(60_000L, dao.findByEpisode("e1")?.playbackDurationMs)
        }

    @Test
    fun `observe maps the stored entity to a domain model`() =
        runTest {
            val dao =
                FakeProgressDao(
                    ProgressEntity(
                        episodeId = "e1",
                        positionMs = 3_000,
                        completed = false,
                        lastPlayedAt = 9,
                        playbackDurationMs = 60_000L,
                    ),
                )
            val repo = ProgressRepositoryImpl(dao) { 0L }

            val progress = repo.observe("e1").first()

            assertEquals("e1", progress?.episodeId)
            assertEquals(3_000L, progress?.positionMs)
            assertEquals(60_000L, progress?.playbackDurationMs)
        }

    @Test
    fun `observe emits null for an episode with no progress`() =
        runTest {
            val progress = ProgressRepositoryImpl(FakeProgressDao()) { 0L }.observe("missing").first()

            assertNull(progress)
        }

    private class FakeProgressDao(
        seed: ProgressEntity? = null,
    ) : ProgressDao {
        private val rows = MutableStateFlow(listOfNotNull(seed).associateBy { it.episodeId })

        override suspend fun upsert(progress: ProgressEntity) {
            rows.value = rows.value + (progress.episodeId to progress)
        }

        override suspend fun findByEpisode(episodeId: String): ProgressEntity? = rows.value[episodeId]

        override fun observeByEpisode(episodeId: String): Flow<ProgressEntity?> = rows.map { it[episodeId] }

        override suspend fun mostRecent(): ProgressEntity? = rows.value.values.maxByOrNull { it.lastPlayedAt }
    }
}
