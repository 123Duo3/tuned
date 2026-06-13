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
    fun `current buffering episode shows loading`() {
        val snapshot =
            episodePlaybackSnapshot(
                episodeId = "e1",
                durationMs = 100_000L,
                progress = null,
                playback =
                    PlaybackState(
                        episodeId = "e1",
                        isPlaying = false,
                        buffering = true,
                        positionMs = 0L,
                        durationMs = 100_000L,
                    ),
            )

        assertEquals(EpisodePlaybackStatus.Loading, snapshot.status)
        assertEquals(0f, snapshot.progress)
        assertEquals(100_000L, snapshot.remainingMs)
    }

    @Test
    fun `replaying completed episode while buffering shows loading`() {
        val snapshot =
            episodePlaybackSnapshot(
                episodeId = "e1",
                durationMs = 100_000L,
                progress = EpisodeProgress("e1", positionMs = 100_000L, completed = true, lastPlayedAt = 1L),
                playback =
                    PlaybackState(
                        episodeId = "e1",
                        isPlaying = false,
                        buffering = true,
                        positionMs = 0L,
                        durationMs = 100_000L,
                    ),
            )

        assertEquals(EpisodePlaybackStatus.Loading, snapshot.status)
        assertEquals(0f, snapshot.progress)
        assertEquals(100_000L, snapshot.remainingMs)
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

    @Test
    fun `stored progress within fifteen seconds of duration is completed`() {
        val snapshot =
            episodePlaybackSnapshot(
                episodeId = "e1",
                durationMs = 100_000L,
                progress = EpisodeProgress("e1", positionMs = 85_000L, completed = false, lastPlayedAt = 1L),
                playback = PlaybackState(),
            )

        assertEquals(EpisodePlaybackStatus.Completed, snapshot.status)
        assertEquals(1f, snapshot.progress)
        assertEquals(null, snapshot.remainingMs)
    }

    @Test
    fun `paused current episode within fifteen seconds of duration is completed`() {
        val snapshot =
            episodePlaybackSnapshot(
                episodeId = "e1",
                durationMs = 100_000L,
                progress = null,
                playback =
                    PlaybackState(
                        episodeId = "e1",
                        isPlaying = false,
                        positionMs = 85_000L,
                        durationMs = 100_000L,
                    ),
            )

        assertEquals(EpisodePlaybackStatus.Completed, snapshot.status)
        assertEquals(1f, snapshot.progress)
        assertEquals(null, snapshot.remainingMs)
    }

    @Test
    fun `playing current episode within fifteen seconds of duration stays playing`() {
        val snapshot =
            episodePlaybackSnapshot(
                episodeId = "e1",
                durationMs = 100_000L,
                progress = null,
                playback =
                    PlaybackState(
                        episodeId = "e1",
                        isPlaying = true,
                        positionMs = 85_000L,
                        durationMs = 100_000L,
                    ),
            )

        assertEquals(EpisodePlaybackStatus.Playing, snapshot.status)
        assertEquals(0.85f, snapshot.progress)
        assertEquals(15_000L, snapshot.remainingMs)
    }

    @Test
    fun `playing current episode wins over buffering`() {
        val snapshot =
            episodePlaybackSnapshot(
                episodeId = "e1",
                durationMs = 100_000L,
                progress = null,
                playback =
                    PlaybackState(
                        episodeId = "e1",
                        isPlaying = true,
                        buffering = true,
                        positionMs = 25_000L,
                        durationMs = 100_000L,
                    ),
            )

        assertEquals(EpisodePlaybackStatus.Playing, snapshot.status)
        assertEquals(0.25f, snapshot.progress)
        assertEquals(75_000L, snapshot.remainingMs)
    }

    @Test
    fun `current episode falls back to episode duration while player duration is unknown`() {
        val snapshot =
            episodePlaybackSnapshot(
                episodeId = "e1",
                durationMs = 100_000L,
                progress = null,
                playback =
                    PlaybackState(
                        episodeId = "e1",
                        isPlaying = true,
                        positionMs = 25_000L,
                        durationMs = null,
                    ),
            )

        assertEquals(EpisodePlaybackStatus.Playing, snapshot.status)
        assertEquals(0.25f, snapshot.progress)
        assertEquals(75_000L, snapshot.remainingMs)
    }

    @Test
    fun `stored playback duration wins over rss duration for non current episode`() {
        val snapshot =
            episodePlaybackSnapshot(
                episodeId = "e1",
                durationMs = 100_000L,
                progress =
                    EpisodeProgress(
                        episodeId = "e1",
                        positionMs = 25_000L,
                        completed = false,
                        lastPlayedAt = 1L,
                        playbackDurationMs = 120_000L,
                    ),
                playback = PlaybackState(),
            )

        assertEquals(EpisodePlaybackStatus.Resume, snapshot.status)
        assertEquals(25_000f / 120_000f, snapshot.progress)
        assertEquals(95_000L, snapshot.remainingMs)
    }
}
