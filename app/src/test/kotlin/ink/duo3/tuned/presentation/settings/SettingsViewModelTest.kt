package ink.duo3.tuned.presentation.settings

import ink.duo3.tuned.domain.model.ThemeSettings
import ink.duo3.tuned.domain.repository.ThemeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `starts without drawing default settings before repository emits`() =
        runTest {
            val viewModel = SettingsViewModel(FakeThemeSettingsRepository())

            assertNull(viewModel.uiState.value.themeSettings)
        }

    @Test
    fun `maps theme settings into ui state`() =
        runTest {
            val repository = FakeThemeSettingsRepository()
            val viewModel = SettingsViewModel(repository)
            val job = launch { viewModel.uiState.collect { it } }
            runCurrent()

            val settings =
                ThemeSettings(
                    followSystemAppearance = false,
                    useDarkMode = true,
                    useMonet = true,
                    monetSeed = 0xFF2196F3.toInt(),
                )
            repository.settings.value = settings
            runCurrent()

            assertEquals(settings, viewModel.uiState.value.themeSettings)
            job.cancel()
        }

    @Test
    fun `setting events update the repository`() =
        runTest {
            val repository = FakeThemeSettingsRepository()
            val viewModel = SettingsViewModel(repository)

            viewModel.setFollowSystemAppearance(false)
            viewModel.setUseDarkMode(true)
            viewModel.setUseMonet(false)
            viewModel.setMonetSeed(0xFF009688.toInt())
            runCurrent()

            assertEquals(
                ThemeSettings(
                    followSystemAppearance = false,
                    useDarkMode = true,
                    useMonet = false,
                    monetSeed = 0xFF009688.toInt(),
                ),
                repository.settings.value,
            )
        }

    private class FakeThemeSettingsRepository : ThemeSettingsRepository {
        val settings = MutableStateFlow(ThemeSettings())

        override val themeSettings: Flow<ThemeSettings> = settings

        override suspend fun setFollowSystemAppearance(followSystemAppearance: Boolean) {
            settings.value = settings.value.copy(followSystemAppearance = followSystemAppearance)
        }

        override suspend fun setUseDarkMode(useDarkMode: Boolean) {
            settings.value = settings.value.copy(useDarkMode = useDarkMode)
        }

        override suspend fun setUseMonet(useMonet: Boolean) {
            settings.value = settings.value.copy(useMonet = useMonet)
        }

        override suspend fun setMonetSeed(monetSeed: Int) {
            settings.value = settings.value.copy(monetSeed = monetSeed)
        }
    }
}
