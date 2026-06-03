package ink.duo3.tuned.presentation.search

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.domain.model.PodcastSearchResult

/**
 * State for the search screen. [query] is the raw field; when it reads as a feed URL
 * ([isUrlQuery]) the screen offers a direct subscribe instead of keyword results.
 * [results] are debounced keyword hits. [subscribingFeedUrl] marks the in-flight subscribe
 * (a tapped result or the typed URL). [addedPodcastId] and [error] are one-shot signals the
 * screen consumes after navigating or showing a snackbar.
 */
data class SearchUiState(
    val query: String = "",
    val isUrlQuery: Boolean = false,
    val isSearching: Boolean = false,
    val results: List<PodcastSearchResult> = emptyList(),
    val subscribingFeedUrl: String? = null,
    val addedPodcastId: String? = null,
    val error: AppError? = null,
)
