package ink.duo3.tuned.ui.componets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ink.duo3.tuned.R
import ink.duo3.tuned.ui.theme.cabinFamily

@Composable
fun LastListenedCard() {
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
                    text = "Last listened",
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
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Row(
                Modifier.padding(16.dp, 0.dp)
            ) {
                Column(
                    Modifier.weight(1f)
                ) {
                    Text(
                        text = "ULSUM RADIO" + " · " + "3 天前发布",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "EP171：一场完美求婚什么样？男女看法大不同？",
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
                ) {}
            }
            Row(
                Modifier.padding(16.dp, 16.dp, 8.dp, 16.dp,)
            ) {
                Button(
                    onClick = { /*TODO*/ },
                    contentPadding = PaddingValues(12.dp, 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_resume_24dp),
                        contentDescription = "Resume",
                        modifier = Modifier
                    )
                    Text(
                        text = "30 minutes left",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(6.dp, 0.dp, 6.dp, 1.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (true) {
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