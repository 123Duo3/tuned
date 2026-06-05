package ink.duo3.tuned.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.duo3.tuned.core.getOrElse
import ink.duo3.tuned.domain.model.Chapter
import ink.duo3.tuned.domain.player.PlaybackController
import ink.duo3.tuned.domain.repository.ChaptersRepository
import ink.duo3.tuned.domain.repository.PodcastRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Drives the full player screen. Combines the controller's [PlaybackState] with the current
 * episode's chapters into one [PlayerUiState]; skip and speed amounts live here as policy.
 * Chapters come from the Podcasting 2.0 document the episode declares (embedded ID3 chapters
 * will feed the same [PlayerUiState.chapters] once added).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModel(
    private val controller: PlaybackController,
    private val podcastRepository: PodcastRepository,
    private val chaptersRepository: ChaptersRepository,
) : ViewModel() {
    val uiState: StateFlow<PlayerUiState> =
        combine(controller.state, chaptersForCurrentEpisode()) { playback, chapters ->
            PlayerUiState(
                playback = playback,
                chapters = chapters,
                currentChapterIndex = currentChapterIndex(chapters, playback.positionMs),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PlayerUiState())

    // Reloads only when the loaded episode changes; emits empty first so a previous episode's
    // chapters don't linger on screen while the new document is fetched.
    private fun chaptersForCurrentEpisode(): Flow<List<Chapter>> =
        controller.state
            .map { it.episodeId }
            .distinctUntilChanged()
            .flatMapLatest { episodeId ->
                if (episodeId == null) {
                    flowOf(emptyList())
                } else {
                    flow {
                        emit(emptyList())
                        emit(loadChapters(episodeId))
                    }
                }
            }

    private suspend fun loadChapters(episodeId: String): List<Chapter> {
        val episode = podcastRepository.observeEpisode(episodeId).first() ?: return emptyList()
        return chaptersRepository.chapters(episode).getOrElse { emptyList() }
    }

    fun playPause() {
        if (controller.state.value.isPlaying) controller.pause() else controller.resume()
    }

    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)

    fun skipBack() = controller.seekBy(-SKIP_BACK_MS)

    fun skipForward() = controller.seekBy(SKIP_FORWARD_MS)

    /** Steps to the next preset speed, wrapping back to the first. */
    fun cycleSpeed() {
        val current = controller.state.value.speed
        val next = SPEEDS.firstOrNull { it > current } ?: SPEEDS.first()
        controller.setSpeed(next)
    }

    /** Preset durations (minutes) offered in the sleep-timer menu. */
    val sleepTimerPresetsMinutes: List<Int> get() = SLEEP_TIMER_MINUTES

    fun startSleepTimer(minutes: Int) = controller.startSleepTimer(minutes * MINUTE_MS)

    fun cancelSleepTimer() = controller.cancelSleepTimer()

    private companion object {
        const val SKIP_BACK_MS = 15_000L
        const val SKIP_FORWARD_MS = 30_000L
        const val MINUTE_MS = 60_000L
        const val STOP_TIMEOUT_MS = 5_000L
        val SPEEDS = listOf(1f, 1.2f, 1.5f, 2f)
        val SLEEP_TIMER_MINUTES = listOf(5, 15, 30, 45, 60)
    }
}

/** The active chapter is the last one whose start is at or before [positionMs]; null before the first. */
private fun currentChapterIndex(
    chapters: List<Chapter>,
    positionMs: Long,
): Int? = chapters.indexOfLast { it.startTimeMs <= positionMs }.takeIf { it >= 0 }
