package ink.duo3.tuned.ui.componets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ink.duo3.tuned.R
import ink.duo3.tuned.data.entity.RecUpdPodEntity
import ink.duo3.tuned.ui.theme.cabinFamily
import ink.duo3.tuned.util.toEpisodeLength
import ink.duo3.tuned.util.toTimeAgo

@Composable
fun RecentlyUpdatedItem(
    episode: RecUpdPodEntity,
    isLastItem: Boolean
) {
    Surface(
        Modifier
            .padding(16.dp, 0.dp)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = (-2).dp,
        shape = if(isLastItem) {
            RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp)
        } else {
            RectangleShape
        }
    ) {
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Column(
            Modifier.padding(0.dp, 16.dp)
        ) {
            Row(Modifier.padding(16.dp, 0.dp)) {
                Column(
                    Modifier.weight(1f)
                ) {
                    Text(
                        text = episode.podName + " · " + episode.pubDateInMs.toTimeAgo(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = cabinFamily,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(44.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(episode.imageUrl)
                            .build(),
                        contentDescription = "",
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Text(
                text = episode.description,
                maxLines = 3,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(16.dp, 8.dp)
            )
            Row(
                Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp)
            ) {
                Button(
                    onClick = { /*TODO*/ },
                    contentPadding = PaddingValues(12.dp, 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier
                    )
                    Text(
                        text = episode.length.toEpisodeLength(),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(6.dp, 0.dp, 6.dp, 1.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (false) {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_playlist_add_check_24dp),
                            contentDescription = "Remove from playlist",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_playlist_add_24dp),
                            contentDescription = "Added to playlist",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (false) {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_download_done_24dp),
                            contentDescription = "Downloaded",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_download_24dp),
                            contentDescription = "Download",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}