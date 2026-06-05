package ink.duo3.tuned.presentation.settings

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.model.InteractionSettings
import ink.duo3.tuned.domain.model.OpmlImportResult
import ink.duo3.tuned.domain.model.ThemeSettings
import ink.duo3.tuned.domain.repository.InteractionSettingsRepository
import ink.duo3.tuned.domain.repository.OpmlRepository
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
import org.junit.Assert.assertTrue
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
            val viewModel =
                SettingsViewModel(
                    FakeThemeSettingsRepository(),
                    FakeInteractionSettingsRepository(),
                    FakeOpmlRepository(),
                )

            assertNull(viewModel.uiState.value.themeSettings)
            assertNull(viewModel.uiState.value.interactionSettings)
        }

    @Test
    fun `maps settings into ui state`() =
        runTest {
            val themeRepository = FakeThemeSettingsRepository()
            val interactionRepository = FakeInteractionSettingsRepository()
            val viewModel =
                SettingsViewModel(
                    themeRepository,
                    interactionRepository,
                    FakeOpmlRepository(),
                )
            val job = launch { viewModel.uiState.collect { it } }
            runCurrent()

            val themeSettings =
                ThemeSettings(
                    followSystemAppearance = false,
                    useDarkMode = true,
                    useMonet = true,
                    monetSeed = 0xFF2196F3.toInt(),
                )
            val interactionSettings = InteractionSettings(hapticFeedbackEnabled = false)
            themeRepository.settings.value = themeSettings
            interactionRepository.settings.value = interactionSettings
            runCurrent()

            assertEquals(themeSettings, viewModel.uiState.value.themeSettings)
            assertEquals(interactionSettings, viewModel.uiState.value.interactionSettings)
            job.cancel()
        }

    @Test
    fun `setting events update the repository`() =
        runTest {
            val themeRepository = FakeThemeSettingsRepository()
            val interactionRepository = FakeInteractionSettingsRepository()
            val viewModel =
                SettingsViewModel(
                    themeRepository,
                    interactionRepository,
                    FakeOpmlRepository(),
                )

            viewModel.setFollowSystemAppearance(false)
            viewModel.setUseDarkMode(true)
            viewModel.setUseMonet(false)
            viewModel.setMonetSeed(0xFF009688.toInt())
            viewModel.setHapticFeedbackEnabled(false)
            runCurrent()

            assertEquals(
                ThemeSettings(
                    followSystemAppearance = false,
                    useDarkMode = true,
                    useMonet = false,
                    monetSeed = 0xFF009688.toInt(),
                ),
                themeRepository.settings.value,
            )
            assertEquals(
                InteractionSettings(hapticFeedbackEnabled = false),
                interactionRepository.settings.value,
            )
        }

    @Test
    fun `successful import surfaces a one-shot Imported event with counts`() =
        runTest {
            val opml = FakeOpmlRepository(importResult = Outcome.Success(OpmlImportResult(imported = 3, failed = 1)))
            val viewModel = settingsViewModel(opml = opml)
            val job = launch { viewModel.uiState.collect { it } }
            runCurrent()

            viewModel.importOpml("<opml/>")
            runCurrent()

            val event = viewModel.uiState.value.opmlEvent
            assertEquals(OpmlEvent.Imported(imported = 3, failed = 1), event)
            assertEquals(false, viewModel.uiState.value.isOpmlBusy)
            job.cancel()
        }

    @Test
    fun `a parse failure surfaces ImportFailed`() =
        runTest {
            val opml = FakeOpmlRepository(importResult = Outcome.Failure(AppError.Parsing()))
            val viewModel = settingsViewModel(opml = opml)
            val job = launch { viewModel.uiState.collect { it } }
            runCurrent()

            viewModel.importOpml("not opml")
            runCurrent()

            assertEquals(OpmlEvent.ImportFailed, viewModel.uiState.value.opmlEvent)
            job.cancel()
        }

    @Test
    fun `export surfaces an ExportReady event carrying the document`() =
        runTest {
            val opml = FakeOpmlRepository(exportResult = Outcome.Success("<opml>document</opml>"))
            val viewModel = settingsViewModel(opml = opml)
            val job = launch { viewModel.uiState.collect { it } }
            runCurrent()

            viewModel.exportOpml()
            runCurrent()

            assertEquals(OpmlEvent.ExportReady("<opml>document</opml>"), viewModel.uiState.value.opmlEvent)
            job.cancel()
        }

    @Test
    fun `consuming an event clears it`() =
        runTest {
            val opml = FakeOpmlRepository(exportResult = Outcome.Failure(AppError.Storage()))
            val viewModel = settingsViewModel(opml = opml)
            val job = launch { viewModel.uiState.collect { it } }
            runCurrent()

            viewModel.exportOpml()
            runCurrent()
            assertEquals(OpmlEvent.ExportFailed, viewModel.uiState.value.opmlEvent)

            viewModel.consumeOpmlEvent()
            runCurrent()
            assertNull(viewModel.uiState.value.opmlEvent)
            job.cancel()
        }

    @Test
    fun `import is ignored while another OPML operation is in flight`() =
        runTest {
            val opml = FakeOpmlRepository(importResult = Outcome.Success(OpmlImportResult(1, 0)))
            val viewModel = settingsViewModel(opml = opml)
            val job = launch { viewModel.uiState.collect { it } }
            runCurrent()

            viewModel.importOpml("<opml/>")
            // Second call before runCurrent lets the first complete: it should be dropped.
            viewModel.importOpml("<opml/>")
            runCurrent()

            assertEquals(1, opml.importCalls)
            assertTrue(viewModel.uiState.value.opmlEvent is OpmlEvent.Imported)
            job.cancel()
        }

    private class FakeOpmlRepository(
        private val importResult: Outcome<OpmlImportResult> = Outcome.Success(OpmlImportResult(0, 0)),
        private val exportResult: Outcome<String> = Outcome.Success(""),
    ) : OpmlRepository {
        var importCalls = 0
            private set

        override suspend fun import(content: String): Outcome<OpmlImportResult> {
            importCalls++
            return importResult
        }

        override suspend fun export(): Outcome<String> = exportResult
    }

    private fun settingsViewModel(opml: OpmlRepository = FakeOpmlRepository()): SettingsViewModel =
        SettingsViewModel(
            FakeThemeSettingsRepository(),
            FakeInteractionSettingsRepository(),
            opml,
        )

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

    private class FakeInteractionSettingsRepository : InteractionSettingsRepository {
        val settings = MutableStateFlow(InteractionSettings())

        override val interactionSettings: Flow<InteractionSettings> = settings

        override suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
            settings.value = settings.value.copy(hapticFeedbackEnabled = enabled)
        }
    }
}
