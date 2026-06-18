package ink.duo3.tuned.ui.components.scaffold

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ink.duo3.tuned.ui.components.shape.tunedRoundedCornerShape
import ink.duo3.tuned.ui.components.text.Text

/**
 * The home screen is a stack of these: a rounded, slightly recessed card with a title
 * row and a content slot. When [onMore] is set, a trailing arrow opens the section's
 * full screen. This is the one place the card's shape/elevation is defined, so the
 * whole app stays visually consistent.
 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null,
    moreContentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = tunedRoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                )
                if (onMore != null) {
                    IconButton(
                        onClick = onMore,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = moreContentDescription,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            content()
        }
    }
}
