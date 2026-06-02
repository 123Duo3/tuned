package ink.duo3.tuned

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import ink.duo3.tuned.navigation.TunedNavGraph
import ink.duo3.tuned.ui.theme.TunedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TunedTheme {
                TunedNavGraph(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
