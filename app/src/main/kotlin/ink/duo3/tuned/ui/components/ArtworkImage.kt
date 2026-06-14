package ink.duo3.tuned.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun ArtworkImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = tunedRoundedCornerShape(16.dp),
) {
    val outline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    Box(
        modifier =
            modifier
                .clip(shape)
                .background(outline)
                .border(ArtworkImageDefaults.BorderWidth, outline, shape),
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

object ArtworkImageDefaults {
    val BorderWidth = 0.1.dp
}
