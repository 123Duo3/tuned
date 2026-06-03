package ink.duo3.tuned.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.repository.PodcastRepository
import ink.duo3.tuned.domain.repository.SearchRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the search screen. Keyword input is debounced into [SearchRepository] keyword
 * queries; an input that reads as a feed URL skips search and is handed straight to the
 * [PodcastRepository] subscribe pipeline. Tapping a result subscribes by its feed URL.
 * The screen collects exactly one [uiState].
 */
class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val podcastRepository: PodcastRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryInput = MutableStateFlow("")

    init {
        observeQuery()
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() {
        viewModelScope.launch {
            queryInput
                .debounce(SEARCH_DEBOUNCE_MILLIS)
                .distinctUntilChanged()
                .collectLatest { runSearch(it) }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query, isUrlQuery = looksLikeFeedUrl(query), error = null) }
        queryInput.value = query
    }

    private suspend fun runSearch(query: String) {
        val term = query.trim()
        if (term.isEmpty() || looksLikeFeedUrl(term)) {
            _uiState.update { it.copy(isSearching = false, results = emptyList()) }
            return
        }
        _uiState.update { it.copy(isSearching = true) }
        _uiState.update {
            when (val outcome = searchRepository.searchPodcasts(term)) {
                is Outcome.Success -> it.copy(isSearching = false, results = outcome.value)
                is Outcome.Failure -> it.copy(isSearching = false, results = emptyList(), error = outcome.error)
            }
        }
    }

    /** Subscribes to [feedUrl] (a tapped result, or the typed URL when omitted). */
    fun subscribe(feedUrl: String = _uiState.value.query) {
        val url = feedUrl.trim()
        if (url.isEmpty() || _uiState.value.subscribingFeedUrl != null) return
        _uiState.update { it.copy(subscribingFeedUrl = url, error = null) }
        viewModelScope.launch {
            _uiState.update {
                when (val outcome = podcastRepository.subscribe(url)) {
                    is Outcome.Success -> it.copy(subscribingFeedUrl = null, addedPodcastId = outcome.value)
                    is Outcome.Failure -> it.copy(subscribingFeedUrl = null, error = outcome.error)
                }
            }
        }
    }

    fun consumeAdded() = _uiState.update { it.copy(addedPodcastId = null) }

    fun consumeError() = _uiState.update { it.copy(error = null) }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 350L
    }
}

/**
 * Treats input as a feed URL (skip keyword search) when it carries a scheme, or is a single
 * dotted token with no whitespace (`example.com/feed`). A bare word or multi-word phrase is a
 * search term.
 */
internal fun looksLikeFeedUrl(input: String): Boolean {
    val trimmed = input.trim()
    return when {
        trimmed.isEmpty() -> false
        trimmed.contains("://") -> true
        trimmed.any(Char::isWhitespace) -> false
        else -> trimmed.contains('.') && !trimmed.startsWith('.') && !trimmed.endsWith('.')
    }
}
