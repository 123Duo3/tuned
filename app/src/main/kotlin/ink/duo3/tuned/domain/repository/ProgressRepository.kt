package ink.duo3.tuned.domain.repository

import ink.duo3.tuned.domain.model.EpisodeProgress
import kotlinx.coroutines.flow.Flow

/**
 * Persisted playback position, separate from [PodcastRepository] because progress
 * churns far more often than subscription data and is written from the playback layer.
 */
interface ProgressRepository {
    /**
     * Where playback should resume for [episodeId]: the stored position, or 0 when there
     * is none or the episode is already [EpisodeProgress.completed] (replay from start).
     */
    suspend fun resumePositionMs(episodeId: String): Long

    /** Upserts the latest position. [completed] marks an episode that reached its end. */
    suspend fun save(
        episodeId: String,
        positionMs: Long,
        completed: Boolean,
    )

    /** This episode's progress, or null if it has never been played. Emits on change. */
    fun observe(episodeId: String): Flow<EpisodeProgress?>
}
