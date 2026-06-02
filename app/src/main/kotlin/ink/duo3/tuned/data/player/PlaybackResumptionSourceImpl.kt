package ink.duo3.tuned.data.player

import ink.duo3.tuned.data.local.dao.EpisodeDao
import ink.duo3.tuned.data.local.dao.PodcastDao
import ink.duo3.tuned.data.local.dao.ProgressDao
import ink.duo3.tuned.domain.player.PlayableEpisode
import ink.duo3.tuned.domain.player.PlaybackResumptionSource

/**
 * Rebuilds a [PlayableEpisode] from Room for playback restoration: the newest unfinished
 * progress row, joined to its episode (for the stream URL and display metadata) and parent
 * podcast (for the title and artwork fallback). Lives in `data/player`, not `data/repository`,
 * because it implements a `domain.player` port rather than a `domain.repository` one.
 */
class PlaybackResumptionSourceImpl(
    private val progressDao: ProgressDao,
    private val episodeDao: EpisodeDao,
    private val podcastDao: PodcastDao,
) : PlaybackResumptionSource {
    override suspend fun lastPlayable(): PlayableEpisode? {
        val progress = progressDao.mostRecent() ?: return null
        val episode = episodeDao.findById(progress.episodeId)
        return episode?.enclosureUrl?.let { streamUrl ->
            val podcast = podcastDao.findById(episode.podcastId)
            PlayableEpisode(
                episodeId = episode.id,
                title = episode.title.orEmpty(),
                podcastTitle = podcast?.title.orEmpty(),
                artworkUrl = episode.artworkUrl ?: podcast?.artworkUrl,
                streamUrl = streamUrl,
                startPositionMs = progress.positionMs,
            )
        }
    }
}
