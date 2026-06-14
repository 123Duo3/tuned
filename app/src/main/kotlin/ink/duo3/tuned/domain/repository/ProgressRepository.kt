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

    /** This episode's current progress snapshot, or null if it has never been played. */
    suspend fun progress(episodeId: String): EpisodeProgress?

    /**
     * Upserts the latest position. [completed] marks an episode that reached its end.
     * [playbackDurationMs] is the latest duration measured from the delivered media file;
     * null preserves the previous measured duration, if any.
     */
    suspend fun save(
        episodeId: String,
        positionMs: Long,
        completed: Boolean,
        playbackDurationMs: Long? = null,
    )

    /** This episode's progress, or null if it has never been played. Emits on change. */
    fun observe(episodeId: String): Flow<EpisodeProgress?>
}
