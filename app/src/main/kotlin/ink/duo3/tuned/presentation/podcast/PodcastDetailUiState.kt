package ink.duo3.tuned.presentation.podcast

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.domain.model.Episode
import ink.duo3.tuned.domain.model.Podcast

/**
 * The single state the podcast detail screen renders. [isLoading] covers the first DB
 * read; once loaded, a null [podcast] means the id is not (or no longer) subscribed.
 * [isRefreshing] tracks an in-flight feed re-fetch; [refreshError] is a one-shot failure
 * the UI maps to a string and then clears via [PodcastDetailViewModel.consumeError].
 */
data class PodcastDetailUiState(
    val isLoading: Boolean = true,
    val podcast: Podcast? = null,
    val episodes: List<Episode> = emptyList(),
    val isRefreshing: Boolean = false,
    val refreshError: AppError? = null,
)
