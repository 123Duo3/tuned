package ink.duo3.tuned.ui.componets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ink.duo3.tuned.data.entity.SubscribedPodEntity
import ink.duo3.tuned.ui.theme.cabinFamily

@Composable
fun SubscribedCard(
    podcasts: List<SubscribedPodEntity>
) {
    Surface(
        modifier = Modifier.padding(16.dp, 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = (-2).dp
    ) {
        Column {
            Row(
            ) {
                Text(
                    text = "Subscribed",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = cabinFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp, 12.dp)
                )
                IconButton(
                    modifier = Modifier.padding(8.dp, 4.dp),
                    onClick = { /*TODO*/ }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            LazyRow(Modifier.padding(bottom = 16.dp)) {
                items(podcasts) {
                    Surface(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(92.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(it.imageUrl)
                                .build(),
                            contentDescription = "",
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                items(1){
                    Spacer(Modifier.width(16.dp))
                }
            }
        }
    }
}