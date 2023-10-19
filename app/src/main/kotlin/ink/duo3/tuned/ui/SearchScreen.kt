package ink.duo3.tuned.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ink.duo3.tuned.R

@Composable
fun SearchScreen(navigationBack: () -> Unit) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val searchText = remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
//        if ((screenHeight < 600.dp) && (screenWidth < 600.dp)) {
//            SearchScreenExtremeCompact(navigationBack)
//        } else if (screenWidth < 600.dp) {
            SearchScreenCompact(
                navigationBack,
                value = searchText.value,
                onValueChange = { searchText.value = it },
                onSearch = {searchText.value = ""}
            )
//        } else if (screenWidth < 840.dp) {
//            SearchScreenMedium(navigationBack)
//        } else {
//            SearchScreenExpanded(navigationBack)
//        }
    }
}

@Composable
fun SearchScreenExtremeCompact(navigationBack: () -> Unit) {
    TODO("Not yet implemented")
}

@Composable
fun SearchScreenCompact(
    navigationBack: () -> Unit,
    value: String,
    onValueChange: (newValue: String) -> Unit,
    onSearch: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()) {
        SearchBar(
            value = value,
            onValueChange = onValueChange,
            navigationBack = navigationBack,
            onSearch = onSearch
        )
        Divider()
        SearchResult()
    }
}

@Composable
fun SearchScreenMedium(navigationBack: () -> Unit) {
    TODO("Not yet implemented")
}

@Composable
fun SearchScreenExpanded(navigationBack: () -> Unit) {
    TODO("Not yet implemented")
}

@Composable
fun SearchBar(
    value: String,
    onValueChange: (newValue: String) -> Unit,
    navigationBack: () -> Unit,
    onSearch: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(8.dp, 16.dp, 8.dp, 8.dp)
    )
    {
        IconButton(
            onClick = navigationBack
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        BasicTextField(
            modifier = Modifier
                .weight(1f)
                .padding(top = 13.dp, bottom = 8.dp),
            value = value,
            onValueChange = onValueChange,
            singleLine = false,
            textStyle = MaterialTheme.typography.bodyLarge
                .copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            keyboardActions = KeyboardActions(
                onAny = {run(onSearch)}
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            decorationBox = {
                Box(
                    modifier = Modifier
                        .weight(1F),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) Text(
                        "Search or add RSS Link",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    it()
                }
            }
        )
        IconButton(
            onClick = onSearch
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RssUrlResult(addSubscription: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.padding(16.dp),
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

            IconButton(onClick = addSubscription) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Subscribe",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(
            text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp, 0.dp)
        )
    }
}

@Composable
fun SearchResult() {
    //TODO: Crossfade result
//    Crossfade(targetState = ) {
//
//    }
    RssUrlResult(addSubscription = {})
}
