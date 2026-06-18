package ink.duo3.tuned.ui.components.text

import androidx.compose.runtime.staticCompositionLocalOf
import ink.duo3.tuned.core.TimeFormatOptions

/**
 * The current timestamp display preference (relative vs precise), provided at the app root from
 * [ink.duo3.tuned.domain.model.InteractionSettings] so any card can format dates consistently
 * without threading the setting through every ViewModel.
 */
val LocalTimeFormatOptions = staticCompositionLocalOf { TimeFormatOptions() }
