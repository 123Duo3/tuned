package ink.duo3.tuned.ui.components.scaffold

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable

/**
 * Pages draw behind the navigation bar. Scrollable content adds bottom clearance as its final
 * element so the end of the page can still be brought above the floating player and system bar.
 */
internal val TunedPageContentInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
