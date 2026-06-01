package ink.duo3.tuned.feature.library

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.domain.model.Podcast

/**
 * The single state the library screen renders. [isLoading] covers the first DB read
 * (distinct from a loaded-but-[podcasts]-empty library, which shows an empty state).
 * [refreshingIds] are podcasts with an in-flight refresh. [refreshError] is a one-shot
 * failure the UI maps to a string and then clears via [LibraryViewModel.consumeError].
 */
data class LibraryUiState(
    val isLoading: Boolean = true,
    val podcasts: List<Podcast> = emptyList(),
    val refreshingIds: Set<String> = emptySet(),
    val refreshError: AppError? = null,
)
