package ink.duo3.tuned.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.prof18.rssparser.model.RssChannel
import ink.duo3.tuned.R
import ink.duo3.tuned.ui.componets.HtmlText
import ink.duo3.tuned.ui.state.SearchUIState
import ink.duo3.tuned.ui.theme.cabinFamily
import ink.duo3.tuned.ui.viewmodel.SearchState
import ink.duo3.tuned.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
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
                state,
                navigationBack,
                value = state.searchFieldValue,
                onValueChange = { viewModel.onSearchFieldValueChanged(it) },
                onSearch = { viewModel.onSearch() },
                addSubscription = { viewModel.subscribe() }
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
    state: SearchUIState,
    navigationBack: () -> Unit,
    value: String,
    onValueChange: (newValue: String) -> Unit,
    onSearch: () -> Unit,
    addSubscription: () -> Unit
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
            state = state,
            addSubscription = addSubscription
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
                        "Search, or RSS Link",
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
fun SearchResult(
    state: SearchUIState,
    addSubscription: () -> Unit
) {
    Crossfade(targetState = state.searchState, label = "") {
        when (it) {
            SearchState.IDLE -> {}
            SearchState.LOADING -> LoadingResult()
            SearchState.SEARCH_SUCCEEDED -> TODO()
            SearchState.RSS_FETCH_SUCCEEDED -> RssUrlResult(
                podcast = state.rssResult,
                addSubscription = addSubscription
            )

            SearchState.SEARCH_NOT_FOUND -> SearchResultEmpty()
            SearchState.RSS_NOT_FOUND -> RssResultEmpty()
            SearchState.NETWORK_ERROR -> NetworkError(errorDescription = state.networkErrorMessage)
        }
    }
}

@Composable
fun RssUrlResult(
    podcast: RssChannel?,
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
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(podcast?.itunesChannelData?.image)
                        .build(),
                    contentDescription = "",
                    contentScale = ContentScale.Fit
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = podcast?.title ?: "No Title",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = cabinFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = podcast?.itunesChannelData?.author ?: "No Author",
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
        Surface(Modifier.padding(16.dp, 0.dp)) {
            HtmlText(
                html = podcast?.description ?: "No description",
                style = MaterialTheme.typography.bodyMedium,
                clickable = true
            )
        }
    }
}

@Composable
fun SearchResultEmpty() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No result",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Try changing to other similar words.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun RssResultEmpty() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_error_48dp),
            contentDescription ="Error",
            tint = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "This URL may not be a valid RSS link.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun NetworkError(errorDescription: String) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_error_48dp),
            contentDescription ="Error",
            tint = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = errorDescription,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun LoadingResult() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
}
