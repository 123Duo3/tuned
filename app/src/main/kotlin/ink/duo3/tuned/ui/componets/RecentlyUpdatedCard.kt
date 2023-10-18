package ink.duo3.tuned.ui.componets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ink.duo3.tuned.ui.theme.cabinFamily

@Composable
fun RecentlyUpdatedCard() {
    Surface(
        modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 0.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = (-2).dp
    ) {
        Column {
            Text(
                text = "Recently updated",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = cabinFamily,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 12.dp)
            )
            (1..20).forEach {
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                RecentlyUpdatedItem(title = "#$it: This is the episode title")
            }
        }
    }
}