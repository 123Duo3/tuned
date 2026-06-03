package ink.duo3.tuned

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ink.duo3.tuned.domain.model.ThemeSettings
import ink.duo3.tuned.domain.repository.ThemeSettingsRepository
import ink.duo3.tuned.navigation.TunedNavGraph
import ink.duo3.tuned.ui.theme.TunedTheme
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val themeSettingsRepository = koinInject<ThemeSettingsRepository>()
            val themeSettings by themeSettingsRepository.themeSettings.collectAsStateWithLifecycle(
                initialValue = ThemeSettings(),
            )
            TunedTheme(themeSettings = themeSettings) {
                TunedNavGraph(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
