package ink.duo3.tuned

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ink.duo3.tuned.core.TimeDisplayMode
import ink.duo3.tuned.core.TimeFormatOptions
import ink.duo3.tuned.domain.model.InteractionSettings
import ink.duo3.tuned.domain.model.ThemeSettings
import ink.duo3.tuned.domain.repository.InteractionSettingsRepository
import ink.duo3.tuned.domain.repository.ThemeSettingsRepository
import ink.duo3.tuned.navigation.TunedNavGraph
import ink.duo3.tuned.ui.components.interaction.LocalTunedHapticFeedbackEnabled
import ink.duo3.tuned.ui.components.text.LocalTimeFormatOptions
import ink.duo3.tuned.ui.theme.TunedTheme
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    private val externalOpmlUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        externalOpmlUri.value = intent.opmlOpenUri()
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val themeSettingsRepository = koinInject<ThemeSettingsRepository>()
            val interactionSettingsRepository = koinInject<InteractionSettingsRepository>()
            val themeSettings by themeSettingsRepository.themeSettings.collectAsStateWithLifecycle(
                initialValue = ThemeSettings(),
            )
            val interactionSettings by interactionSettingsRepository.interactionSettings.collectAsStateWithLifecycle(
                initialValue = InteractionSettings(),
            )
            TunedTheme(themeSettings = themeSettings) {
                CompositionLocalProvider(
                    LocalTunedHapticFeedbackEnabled provides interactionSettings.hapticFeedbackEnabled,
                    LocalTimeFormatOptions provides
                        TimeFormatOptions(
                            mode =
                                if (interactionSettings.usePreciseTime) {
                                    TimeDisplayMode.PRECISE
                                } else {
                                    TimeDisplayMode.RELATIVE
                                },
                        ),
                ) {
                    TunedNavGraph(
                        externalOpmlUri = externalOpmlUri.value,
                        onExternalOpmlUriConsumed = { externalOpmlUri.value = null },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalOpmlUri.value = intent.opmlOpenUri()
    }
}

private fun Intent.opmlOpenUri(): Uri? = data.takeIf { action == Intent.ACTION_VIEW }
