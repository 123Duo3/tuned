package ink.duo3.tuned.domain.player

import ink.duo3.tuned.domain.model.EpisodeProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodePlaybackSnapshotFactoryTest {
    @Test
    fun `completed current episode stays completed when playback is paused`() {
        val snapshot =
            episodePlaybackSnapshot(
                episodeId = "e1",
                durationMs = 100_000L,
                progress = EpisodeProgress("e1", positionMs = 100_000L, completed = true, lastPlayedAt = 1L),
                playback =
                    PlaybackState(
                        episodeId = "e1",
                        isPlaying = false,
                        positionMs = 100_000L,
                        durationMs = 100_000L,
                    ),
            )

        assertEquals(EpisodePlaybackStatus.Completed, snapshot.status)
        assertEquals(1f, snapshot.progress)
        assertEquals(null, snapshot.remainingMs)
    }

    @Test
    fun `replaying a previously completed current episode shows playing`() {
        val snapshot =
            episodePlaybackSnapshot(
                episodeId = "e1",
                durationMs = 100_000L,
                progress = EpisodeProgress("e1", positionMs = 100_000L, completed = true, lastPlayedAt = 1L),
                playback =
                    PlaybackState(
                        episodeId = "e1",
                        isPlaying = true,
                        positionMs = 25_000L,
                        durationMs = 100_000L,
                    ),
            )

        assertEquals(EpisodePlaybackStatus.Playing, snapshot.status)
        assertEquals(0.25f, snapshot.progress)
        assertEquals(75_000L, snapshot.remainingMs)
    }

    @Test
    fun `current episode at duration is completed even before progress flow catches up`() {
        val snapshot =
            episodePlaybackSnapshot(
                episodeId = "e1",
                durationMs = 100_000L,
                progress = null,
                playback =
                    PlaybackState(
                        episodeId = "e1",
                        isPlaying = false,
                        positionMs = 100_000L,
                        durationMs = 100_000L,
                    ),
            )

        assertEquals(EpisodePlaybackStatus.Completed, snapshot.status)
        assertEquals(1f, snapshot.progress)
        assertEquals(null, snapshot.remainingMs)
    }
}
