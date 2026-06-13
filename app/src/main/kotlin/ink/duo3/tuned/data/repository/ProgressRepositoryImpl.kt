package ink.duo3.tuned.data.repository

import ink.duo3.tuned.data.local.asProgress
import ink.duo3.tuned.data.local.dao.ProgressDao
import ink.duo3.tuned.data.local.entity.ProgressEntity
import ink.duo3.tuned.domain.repository.ProgressRepository

/**
 * Room-backed playback progress. [now] is injected so the persisted `lastPlayedAt`
 * stays deterministic under test.
 */
class ProgressRepositoryImpl(
    private val progressDao: ProgressDao,
    private val now: () -> Long,
) : ProgressRepository {
    override suspend fun resumePositionMs(episodeId: String): Long =
        progressDao
            .findByEpisode(episodeId)
            ?.takeUnless { it.completed }
            ?.positionMs
            ?: 0L

    override suspend fun save(
        episodeId: String,
        positionMs: Long,
        completed: Boolean,
        playbackDurationMs: Long?,
    ) {
        val existing = progressDao.findByEpisode(episodeId)
        progressDao.upsert(
            ProgressEntity(
                episodeId = episodeId,
                positionMs = positionMs,
                completed = completed,
                lastPlayedAt = now(),
                playbackDurationMs = playbackDurationMs?.takeIf { it > 0L } ?: existing?.playbackDurationMs,
            ),
        )
    }

    override fun observe(episodeId: String) = progressDao.observeByEpisode(episodeId).asProgress()
}
