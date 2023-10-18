package ink.duo3.tuned.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ink.duo3.tuned.R
import ink.duo3.tuned.ui.theme.cabinFamily

@Composable
fun InitialSubscriptScreen(navigationDone: () -> Unit) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val subscriptionCount = remember{ mutableIntStateOf(0) }
    val addSubscription = { subscriptionCount.intValue += 1 }
    val removeSubscription = { subscriptionCount.intValue -= 1 }
    val doneEnabled = (subscriptionCount.intValue > 0)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        if ((screenHeight < 600.dp) && (screenWidth < 600.dp)) {
            InitialSubscriptScreenExtremeCompact(
                navigationDone,
                addSubscription,
                removeSubscription,
                doneEnabled,
                subscriptionCount.intValue
            )
        } else if (screenWidth < 600.dp) {
            InitialSubscriptScreenCompact(
                navigationDone,
                addSubscription,
                removeSubscription,
                doneEnabled,
                subscriptionCount.intValue
            )
        } else if (screenWidth < 840.dp) {
            InitialSubscriptScreenMedium(
                navigationDone,
                addSubscription,
                removeSubscription,
                doneEnabled,
                subscriptionCount.intValue
            )
        } else {
            InitialSubscriptScreenExpanded(
                navigationDone,
                addSubscription,
                removeSubscription,
                doneEnabled,
                subscriptionCount.intValue
            )
        }
    }
}

@Composable
fun InitialSubscriptScreenExtremeCompact(
    navigationDone: () -> Unit,
    addSubscription: () -> Unit,
    removeSubscription: () -> Unit,
    doneEnabled: Boolean,
    subscriptionCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Text(
            text = stringResource(id = R.string.initial_subscription),
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = cabinFamily,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SubscribedList(
            modifier = Modifier.weight(1f),
            removeSubscription,
            addSubscription,
            subscriptionCount
        )

        SubscribedActionButtons(
            modifier = Modifier.padding(16.dp),
            enableExtraPadding = false,
            navigationDone,
            doneEnabled
        )
    }
}

@Composable
fun InitialSubscriptScreenCompact(
    navigationDone: () -> Unit,
    addSubscription: () -> Unit,
    removeSubscription: () -> Unit,
    doneEnabled: Boolean,
    subscriptionCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Text(
            text = stringResource(id = R.string.initial_subscription),
            style = MaterialTheme.typography.displaySmall,
            fontFamily = cabinFamily,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp)
        )

        SubscribedList(
            modifier = Modifier.weight(1f),
            removeSubscription,
            addSubscription,
            subscriptionCount
        )

        SubscribedActionButtons(
            modifier = Modifier.padding(16.dp),
            navigationDone = navigationDone,
            doneEnabled = doneEnabled
        )
    }
}

@Composable
fun InitialSubscriptScreenMedium(
    navigationDone: () -> Unit,
    addSubscription: () -> Unit,
    removeSubscription: () -> Unit,
    doneEnabled: Boolean,
    subscriptionCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.5f),
            //verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.initial_subscription),
                style = MaterialTheme.typography.displaySmall,
                fontFamily = cabinFamily,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.5f)
        ) {
            SubscribedList(
                modifier = Modifier.weight(1f),
                removeSubscription,
                addSubscription,
                subscriptionCount
            )
            SubscribedActionButtons(
                modifier = Modifier.padding(16.dp),
                navigationDone = navigationDone,
                doneEnabled = doneEnabled
            )
        }
    }
}

@Composable
fun InitialSubscriptScreenExpanded(
    navigationDone: () -> Unit,
    addSubscription: () -> Unit,
    removeSubscription: () -> Unit,
    doneEnabled: Boolean,
    subscriptionCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.4f),
            //verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.initial_subscription),
                style = MaterialTheme.typography.displaySmall,
                fontFamily = cabinFamily,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.6f)
        ) {
            SubscribedList(
                modifier = Modifier.weight(1f),
                addSubscription,
                removeSubscription,
                subscriptionCount
            )
            SubscribedActionButtons(
                modifier = Modifier.padding(16.dp),
                navigationDone = navigationDone,
                doneEnabled = doneEnabled
            )
        }
    }
}

@Composable
fun SubscribedList(
    modifier: Modifier,
    removeSubscription: () -> Unit,
    addSubscription: () -> Unit,
    subscriptionCount: Int
) {
    Column(modifier = modifier) {
        Divider(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(subscriptionCount) {
                SubscribeItem(removeSubscription)
            }
            items(1) {
                AddSubscribeItem(addSubscription)
            }
        }

        Divider(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
fun SubscribeItem(removeSubscription: () -> Unit) {
    Surface(
        Modifier.padding(8.dp, 8.dp, 8.dp, 0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            Modifier.padding(8.dp, 8.dp, 8.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(56.dp),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {}

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = "字谈字畅",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "The Type",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = removeSubscription) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete_24dp),
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddSubscribeItem (addSubscription: () -> Unit) {
    Surface(
        Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = addSubscription),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .padding(end = 16.dp),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 2.dp
            ) {
                Icon(
                    modifier = Modifier.padding(16.dp),
                    imageVector = Icons.Default.Add,
                    tint = MaterialTheme.colorScheme.outline,
                    contentDescription = null
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.series_add),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SubscribedActionButtons(
    modifier: Modifier,
    enableExtraPadding: Boolean = true,
    navigationDone: () -> Unit,
    doneEnabled: Boolean
) {
    Row(modifier) {
        Row(Modifier.weight(0.5f)) {
            TextButton(onClick = navigationDone) {
                Text(
                    modifier = Modifier.padding(extraButtonTextPadding(enableExtraPadding)),
                    text = stringResource(id = R.string.button_skip),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Button(
            modifier = Modifier.weight(0.5f),
            onClick = navigationDone,
            enabled = doneEnabled
        ) {
            Text(
                modifier = Modifier.padding(extraButtonTextPadding(enableExtraPadding)),
                text = stringResource(id = R.string.button_done),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
