package ink.duo3.tuned.presentation.search

import ink.duo3.tuned.core.AppError

/**
 * State for the add-by-URL screen. [query] is the raw text field (an RSS feed URL for
 * now; keyword search joins later). [isSubmitting] gates a second tap while the feed is
 * being fetched. [addedPodcastId] and [error] are one-shot signals the screen consumes
 * after navigating to the podcast or showing a snackbar.
 */
data class SearchUiState(
    val query: String = "",
    val isSubmitting: Boolean = false,
    val addedPodcastId: String? = null,
    val error: AppError? = null,
)
