package ink.duo3.tuned.domain.player

import ink.duo3.tuned.domain.model.EpisodeProgress

fun episodePlaybackSnapshot(
    episodeId: String,
    durationMs: Long?,
    progress: EpisodeProgress?,
    playback: PlaybackState,
): EpisodePlaybackSnapshot {
    val isCurrent = playback.episodeId == episodeId
    val effectiveDurationMs = effectiveDurationMs(isCurrent, durationMs, playback)
    val completed = isCompleted(isCurrent, effectiveDurationMs, progress, playback)
    val positionMs = displayPositionMs(isCurrent, completed, effectiveDurationMs, progress, playback)
    return EpisodePlaybackSnapshot(
        status = playbackStatus(isCurrent, playback.isPlaying, completed, positionMs),
        progress = progressFraction(positionMs = positionMs, durationMs = effectiveDurationMs, completed = completed),
        remainingMs = remainingMs(positionMs = positionMs, durationMs = effectiveDurationMs, completed = completed),
    )
}

private fun effectiveDurationMs(
    isCurrent: Boolean,
    durationMs: Long?,
    playback: PlaybackState,
): Long? =
    playback.durationMs
        .takeIf { isCurrent && it > 0L }
        ?: durationMs

private fun isCompleted(
    isCurrent: Boolean,
    durationMs: Long?,
    progress: EpisodeProgress?,
    playback: PlaybackState,
): Boolean {
    val liveEnded =
        isCurrent &&
            !playback.isPlaying &&
            durationMs != null &&
            durationMs > 0L &&
            playback.positionMs >= durationMs
    return (progress?.completed == true || liveEnded) && !(isCurrent && playback.isPlaying)
}

private fun displayPositionMs(
    isCurrent: Boolean,
    completed: Boolean,
    durationMs: Long?,
    progress: EpisodeProgress?,
    playback: PlaybackState,
): Long =
    when {
        isCurrent -> playback.positionMs
        completed -> durationMs ?: progress?.positionMs ?: 0L
        else -> progress?.positionMs ?: 0L
    }

private fun playbackStatus(
    isCurrent: Boolean,
    isPlaying: Boolean,
    completed: Boolean,
    positionMs: Long,
): EpisodePlaybackStatus =
    when {
        isCurrent && isPlaying -> EpisodePlaybackStatus.Playing
        completed -> EpisodePlaybackStatus.Completed
        positionMs > 0L -> EpisodePlaybackStatus.Resume
        else -> EpisodePlaybackStatus.Unplayed
    }

private fun progressFraction(
    positionMs: Long,
    durationMs: Long?,
    completed: Boolean,
): Float =
    when {
        completed -> 1f
        durationMs == null || durationMs <= 0L -> 0f
        else -> (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

private fun remainingMs(
    positionMs: Long,
    durationMs: Long?,
    completed: Boolean,
): Long? =
    when {
        completed || durationMs == null || durationMs <= 0L -> null
        else -> (durationMs - positionMs).coerceAtLeast(0L)
    }
