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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prof18.rssparser.model.RssChannel
import ink.duo3.tuned.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    navigationBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
//        if ((screenHeight < 600.dp) && (screenWidth < 600.dp)) {
//            SearchScreenExtremeCompact(navigationBack)
//        } else if (screenWidth < 600.dp) {
            SearchScreenCompact(
                state.searchResult,
                state.rssResult,
                navigationBack,
                value = state.searchFieldValue,
                onValueChange = { viewModel.onSearchFieldValueChanged(it) },
                onSearch = { viewModel.onSearch() }
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
    searchResult: List<RssChannel>,
    rssResult: RssChannel?,
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
        SearchResult(
            searchResult,
            rssResult
        )
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
fun RssUrlResult(
    podcast: RssChannel,
    addSubscription: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())) {
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
                    text = podcast.title!!,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = podcast.itunesChannelData?.author!!,
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
            text = podcast.description!!,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp, 0.dp)
        )
    }
}

@Composable
fun SearchResult(
    searchResult: List<RssChannel>,
    rssResult: RssChannel?
) {
    //TODO: Crossfade result
//    Crossfade(targetState = ) {
//
//    }
    //TODO: Empty state screen
    if (rssResult != null) {
        RssUrlResult(
            podcast = rssResult,
            addSubscription = {}
        )
    }
}
