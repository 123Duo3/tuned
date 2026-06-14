package ink.duo3.tuned.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ink.duo3.tuned.R
import ink.duo3.tuned.domain.model.PodcastSearchResult
import ink.duo3.tuned.presentation.search.SearchUiState
import ink.duo3.tuned.presentation.search.SearchViewModel
import ink.duo3.tuned.ui.components.ArtworkImage
import ink.duo3.tuned.ui.components.LocalMiniPlayerBottomClearance
import ink.duo3.tuned.ui.components.Text
import ink.duo3.tuned.ui.components.TunedPageContentInsets
import ink.duo3.tuned.ui.components.appErrorMessage
import ink.duo3.tuned.ui.components.tunedRoundedCornerShape

/** Keyword search (iTunes) plus add-by-URL: a feed address is subscribed directly, a phrase is searched. */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onPodcastAdded: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = state.error?.let { appErrorMessage(it) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.consumeError()
        }
    }
    LaunchedEffect(state.addedPodcastId) {
        state.addedPodcastId?.let {
            viewModel.consumeAdded()
            onPodcastAdded(it)
        }
    }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = TunedPageContentInsets,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val keyboardController = LocalSoftwareKeyboardController.current
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchField(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                onClear = { viewModel.onQueryChange("") },
                onSubmit = {
                    keyboardController?.hide()
                    if (state.isUrlQuery) viewModel.subscribe()
                },
            )
            SearchContent(state = state, onSubscribe = viewModel::subscribe)
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.search_field_placeholder)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.search_field_clear))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
    )
}

@Composable
private fun SearchContent(
    state: SearchUiState,
    onSubscribe: (String) -> Unit,
) {
    val term = state.query.trim()
    when {
        state.isUrlQuery ->
            UrlSubscribePrompt(
                isSubscribing = state.subscribingFeedUrl != null,
                onSubscribe = { onSubscribe(state.query) },
            )
        state.isSearching && state.results.isEmpty() ->
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        state.results.isNotEmpty() ->
            ResultList(
                results = state.results,
                subscribingFeedUrl = state.subscribingFeedUrl,
                onSubscribe = onSubscribe,
            )
        term.isNotEmpty() && !state.isSearching ->
            CenteredMessage(stringResource(R.string.search_no_results, term))
        else -> CenteredMessage(stringResource(R.string.search_prompt))
    }
}

@Composable
private fun ResultList(
    results: List<PodcastSearchResult>,
    subscribingFeedUrl: String?,
    onSubscribe: (String) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalMiniPlayerBottomClearance.current + 8.dp),
    ) {
        items(results, key = { it.feedUrl }) { result ->
            ResultRow(
                result = result,
                isSubscribing = subscribingFeedUrl == result.feedUrl,
                onClick = { onSubscribe(result.feedUrl) },
            )
        }
    }
}

@Composable
private fun ResultRow(
    result: PodcastSearchResult,
    isSubscribing: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ArtworkImage(
            model = result.artworkUrl,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            shape = tunedRoundedCornerShape(12.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                result.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            result.subtitle()?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isSubscribing) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.search_subscribe))
        }
    }
}

@Composable
private fun PodcastSearchResult.subtitle(): String? {
    val episodes = episodeCount?.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.search_episode_count, it, it) }
    return listOfNotNull(author, episodes).takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
private fun UrlSubscribePrompt(
    isSubscribing: Boolean,
    onSubscribe: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = onSubscribe, enabled = !isSubscribing, modifier = Modifier.fillMaxWidth()) {
            if (isSubscribing) {
                CircularProgressIndicator(Modifier.padding(end = 8.dp).size(18.dp), strokeWidth = 2.dp)
            }
            Text(stringResource(R.string.search_subscribe_feed))
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
