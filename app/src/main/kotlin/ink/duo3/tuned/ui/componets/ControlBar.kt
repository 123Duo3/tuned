package ink.duo3.tuned.ui.componets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ControlBar(bottomPadding: Dp){
    Surface(
        Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 14.dp
    ) {
        Box(
            Modifier
                .height(4.dp)
                .fillMaxWidth()
        ) {
            Surface(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart)
                    .height(4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            ) {}
            Surface(
                Modifier
                    .fillMaxWidth(0.35f)
                    .align(Alignment.CenterStart)
                    .height(4.dp),
                color = MaterialTheme.colorScheme.secondary
            ) {}
        }
        Row(
            Modifier.padding(16.dp).padding(bottom = bottomPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(44.dp),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {}
            Column(
                Modifier.weight(1f)
            ) {
                Text(
                    text = "#123: This is the episode title",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                )
                Text(
                    text = "Series name",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                )
            }
            IconButton(onClick = { /*TODO*/ }) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play")
            }
        }
    }
}