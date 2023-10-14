package ink.duo3.tuned.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ink.duo3.tuned.R
import ink.duo3.tuned.ui.theme.cabinFamily

@Composable
fun InitialSubscriptScreen() {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        if ((screenHeight < 600.dp) && (screenWidth < 600.dp)) {
            InitialSubscriptScreenExtremeCompact()
        } else if (screenWidth < 600.dp) {
            InitialSubscriptScreenCompact()
        } else if (screenWidth < 840.dp) {
            InitialSubscriptScreenMedium()
        } else {
            InitialSubscriptScreenExpanded()
        }
    }
}

@Composable
fun InitialSubscriptScreenExtremeCompact() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.initial_subscription),
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = cabinFamily,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SubscribedList(modifier = Modifier.weight(1f))

        SubscribedActionButtons(modifier = Modifier, false)
    }
}

@Composable
fun InitialSubscriptScreenCompact() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.initial_subscription),
            style = MaterialTheme.typography.displaySmall,
            fontFamily = cabinFamily,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        SubscribedList(modifier = Modifier.weight(1f))

        SubscribedActionButtons(modifier = Modifier)
    }
}

@Composable
fun InitialSubscriptScreenMedium() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
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
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.5f)
        ) {
            SubscribedList(modifier = Modifier.weight(1f))
            SubscribedActionButtons(modifier = Modifier)
        }
    }
}

@Composable
fun InitialSubscriptScreenExpanded() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
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
            SubscribedList(modifier = Modifier.weight(1f))
            SubscribedActionButtons(modifier = Modifier)
        }
    }
}

@Composable
fun SubscribeItem() {
    Row(
        Modifier.padding(top = 16.dp),
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
        
        IconButton(onClick = { /*TODO*/ }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_delete_24dp),
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun AddSubscribeItem () {
    Row(
        Modifier.padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .padding(end = 16.dp),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.inverseOnSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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

@Composable
fun SubscribedList(modifier: Modifier) {
    Column(modifier = modifier) {
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(10) {
                SubscribeItem()
            }
            items(1) {
                AddSubscribeItem()
            }
        }

        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
fun SubscribedActionButtons(modifier: Modifier, enableExtraPadding: Boolean = true) {
    Row(modifier.padding(top = 16.dp)) {
        Row(Modifier.weight(0.5f)) {
            TextButton(onClick = { /*TODO*/ }) {
                Text(
                    modifier = Modifier.padding(extraButtonTextPadding(enableExtraPadding)),
                    text = stringResource(id = R.string.button_skip),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Button(
            modifier = Modifier.weight(0.5f),
            onClick = { /*TODO*/ }
        ) {
            Text(
                modifier = Modifier.padding(extraButtonTextPadding(enableExtraPadding)),
                text = stringResource(id = R.string.button_done),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
