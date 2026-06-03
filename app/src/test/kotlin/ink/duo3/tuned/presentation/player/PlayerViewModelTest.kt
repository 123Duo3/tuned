package ink.duo3.tuned.presentation.player

import ink.duo3.tuned.domain.player.PlayableEpisode
import ink.duo3.tuned.domain.player.PlaybackController
import ink.duo3.tuned.domain.player.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PlayerViewModelTest {
    @Test
    fun `uiState is the controller state`() {
        val controller = FakePlaybackController()
        val vm = PlayerViewModel(controller)
        assertSame(controller.state, vm.uiState)
    }

    @Test
    fun `playPause pauses while playing and resumes while paused`() {
        val controller = FakePlaybackController()
        val vm = PlayerViewModel(controller)

        controller.emit(PlaybackState(episodeId = "e1", isPlaying = true))
        vm.playPause()
        assertEquals(listOf("pause"), controller.calls)

        controller.emit(PlaybackState(episodeId = "e1", isPlaying = false))
        vm.playPause()
        assertEquals(listOf("pause", "resume"), controller.calls)
    }

    @Test
    fun `skip maps to fixed seek deltas`() {
        val controller = FakePlaybackController()
        val vm = PlayerViewModel(controller)

        vm.skipBack()
        vm.skipForward()

        assertEquals(listOf(-15_000L, 30_000L), controller.seekDeltas)
    }

    @Test
    fun `seekTo delegates to the controller`() {
        val controller = FakePlaybackController()
        val vm = PlayerViewModel(controller)

        vm.seekTo(42_000L)

        assertEquals(listOf(42_000L), controller.seekPositions)
    }

    @Test
    fun `cycleSpeed steps up then wraps to the first preset`() {
        val controller = FakePlaybackController()
        val vm = PlayerViewModel(controller)

        controller.emit(PlaybackState(speed = 1f))
        vm.cycleSpeed()
        assertEquals(1.2f, controller.speeds.last())

        controller.emit(PlaybackState(speed = 2f))
        vm.cycleSpeed()
        assertEquals(1f, controller.speeds.last())
    }

    @Test
    fun `startSleepTimer converts preset minutes to milliseconds`() {
        val controller = FakePlaybackController()
        val vm = PlayerViewModel(controller)

        vm.startSleepTimer(30)

        assertEquals(listOf(30 * 60_000L), controller.sleepDurations)
    }

    @Test
    fun `cancelSleepTimer delegates to the controller`() {
        val controller = FakePlaybackController()
        val vm = PlayerViewModel(controller)

        vm.cancelSleepTimer()

        assertEquals(listOf("cancelSleepTimer"), controller.calls)
    }

    private class FakePlaybackController : PlaybackController {
        private val _state = MutableStateFlow(PlaybackState())
        override val state: StateFlow<PlaybackState> = _state

        val calls = mutableListOf<String>()
        val seekPositions = mutableListOf<Long>()
        val seekDeltas = mutableListOf<Long>()
        val speeds = mutableListOf<Float>()
        val sleepDurations = mutableListOf<Long>()

        fun emit(state: PlaybackState) {
            _state.value = state
        }

        override fun play(item: PlayableEpisode) {
            calls += "play"
        }

        override fun resume() {
            calls += "resume"
        }

        override fun pause() {
            calls += "pause"
        }

        override fun seekTo(positionMs: Long) {
            seekPositions += positionMs
        }

        override fun seekBy(deltaMs: Long) {
            seekDeltas += deltaMs
        }

        override fun setSpeed(speed: Float) {
            speeds += speed
        }

        override fun stop() {
            calls += "stop"
        }

        override fun startSleepTimer(durationMs: Long) {
            sleepDurations += durationMs
        }

        override fun cancelSleepTimer() {
            calls += "cancelSleepTimer"
        }
    }
}
