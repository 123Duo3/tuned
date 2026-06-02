package ink.duo3.tuned.presentation.player

import androidx.lifecycle.ViewModel
import ink.duo3.tuned.domain.player.PlaybackController
import ink.duo3.tuned.domain.player.PlaybackState
import kotlinx.coroutines.flow.StateFlow

/**
 * Drives the full player screen. The UI renders directly from the controller's
 * [PlaybackState] — there is no separate UiState to keep in sync. Event functions
 * delegate to the [PlaybackController]; skip and speed amounts live here as policy.
 */
class PlayerViewModel(
    private val controller: PlaybackController,
) : ViewModel() {
    val uiState: StateFlow<PlaybackState> = controller.state

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

    private companion object {
        const val SKIP_BACK_MS = 15_000L
        const val SKIP_FORWARD_MS = 30_000L
        val SPEEDS = listOf(1f, 1.2f, 1.5f, 2f)
    }
}
