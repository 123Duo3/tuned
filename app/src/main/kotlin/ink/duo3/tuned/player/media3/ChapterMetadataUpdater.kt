package ink.duo3.tuned.player.media3

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import ink.duo3.tuned.core.getOrElse
import ink.duo3.tuned.domain.model.Chapter
import ink.duo3.tuned.domain.repository.ChaptersRepository
import ink.duo3.tuned.domain.repository.PodcastRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Keeps session/notification metadata aligned with the chapter at the player's live position. */
internal class ChapterMetadataUpdater(
    private val player: Player,
    private val podcastRepository: PodcastRepository,
    private val chaptersRepository: ChaptersRepository,
) : Player.Listener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loadJob: Job? = null
    private var tickerJob: Job? = null
    private var episodeMetadata: EpisodeNotificationMetadata? = null
    private var chapters = emptyList<Chapter>()
    private var appliedChapterIndex = CHAPTER_INDEX_UNSET

    fun attach() {
        player.addListener(this)
        loadCurrentEpisode()
        updateTicker()
    }

    fun detach() {
        player.removeListener(this)
        scope.cancel()
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        if (mediaItem?.mediaId != episodeMetadata?.episodeId) loadCurrentEpisode()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) = updateTicker()

    override fun onPlaybackStateChanged(playbackState: Int) = updateTicker()

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) = updateMetadata()

    private fun loadCurrentEpisode() {
        loadJob?.cancel()
        val mediaItem = player.currentMediaItem
        val episodeId = mediaItem?.mediaId?.takeIf(String::isNotEmpty)
        episodeMetadata = mediaItem?.toEpisodeNotificationMetadata()
        chapters = emptyList()
        appliedChapterIndex = CHAPTER_INDEX_UNSET
        if (episodeId == null) return

        loadJob =
            scope.launch {
                val episode = podcastRepository.observeEpisode(episodeId).first() ?: return@launch
                val loaded = chaptersRepository.chapters(episode).getOrElse { emptyList() }
                if (player.currentMediaItem?.mediaId != episodeId) return@launch
                chapters = loaded
                updateMetadata()
            }
    }

    private fun updateTicker() {
        if (!player.isPlaying) {
            tickerJob?.cancel()
            tickerJob = null
            updateMetadata()
            return
        }
        if (tickerJob?.isActive == true) return
        tickerJob =
            scope.launch {
                while (isActive) {
                    updateMetadata()
                    delay(CHAPTER_TICK_MS)
                }
            }
    }

    private fun updateMetadata() {
        val base = episodeMetadata
        val currentItem = player.currentMediaItem
        if (base == null || currentItem == null) return
        val index = chapters.indexOfLast { it.startTimeMs <= player.currentPosition }
        if (index != appliedChapterIndex && currentItem.mediaId == base.episodeId) {
            val presentation =
                chapterNotificationPresentation(
                    chapters = chapters,
                    positionMs = player.currentPosition,
                    episodeTitle = base.episodeTitle,
                    podcastTitle = base.podcastTitle,
                    episodeArtworkUrl = base.artworkUrl,
                )
            val metadata =
                currentItem.mediaMetadata
                    .buildUpon()
                    .setTitle(presentation.title)
                    .setSubtitle(presentation.subtitle)
                    .setArtist(base.podcastTitle)
                    .setAlbumTitle(base.podcastTitle)
                    .setArtworkUri(presentation.artworkUrl?.let(Uri::parse))
                    .build()
            val updatedItem = currentItem.buildUpon().setMediaMetadata(metadata).build()
            appliedChapterIndex = index
            player.replaceMediaItem(player.currentMediaItemIndex, updatedItem)
        }
    }
}

private fun MediaItem.toEpisodeNotificationMetadata(): EpisodeNotificationMetadata {
    val extras = mediaMetadata.extras
    return EpisodeNotificationMetadata(
        episodeId = mediaId,
        episodeTitle = extras?.getString(METADATA_EPISODE_TITLE) ?: mediaMetadata.title?.toString().orEmpty(),
        podcastTitle = extras?.getString(METADATA_PODCAST_TITLE) ?: mediaMetadata.artist?.toString().orEmpty(),
        artworkUrl = extras?.getString(METADATA_EPISODE_ARTWORK) ?: mediaMetadata.artworkUri?.toString(),
    )
}

private data class EpisodeNotificationMetadata(
    val episodeId: String,
    val episodeTitle: String,
    val podcastTitle: String,
    val artworkUrl: String?,
)

internal fun chapterNotificationPresentation(
    chapters: List<Chapter>,
    positionMs: Long,
    episodeTitle: String,
    podcastTitle: String,
    episodeArtworkUrl: String?,
): ChapterNotificationPresentation {
    val chapter = chapters.lastOrNull { it.startTimeMs <= positionMs }
    val chapterTitle = chapter?.title?.takeIf(String::isNotBlank)
    return ChapterNotificationPresentation(
        title = chapterTitle ?: episodeTitle,
        subtitle = if (chapterTitle == null) podcastTitle else episodeTitle,
        artworkUrl = chapter?.imageUrl ?: episodeArtworkUrl,
    )
}

internal data class ChapterNotificationPresentation(
    val title: String,
    val subtitle: String,
    val artworkUrl: String?,
)

internal const val METADATA_EPISODE_TITLE = "ink.duo3.tuned.metadata.EPISODE_TITLE"
internal const val METADATA_PODCAST_TITLE = "ink.duo3.tuned.metadata.PODCAST_TITLE"
internal const val METADATA_EPISODE_ARTWORK = "ink.duo3.tuned.metadata.EPISODE_ARTWORK"

private const val CHAPTER_INDEX_UNSET = Int.MIN_VALUE
private const val CHAPTER_TICK_MS = 500L
