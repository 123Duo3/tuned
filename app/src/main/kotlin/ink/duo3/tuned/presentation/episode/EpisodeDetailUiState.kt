package ink.duo3.tuned.presentation.episode

import ink.duo3.tuned.domain.model.Episode
import ink.duo3.tuned.domain.model.Podcast

/**
 * One episode's detail view. [podcast] supplies the parent's title and fallback
 * artwork; both it and [episode] are null until the Room flows emit, or stay null
 * if the episode is not stored. [isLoading] is the pre-first-emission state.
 */
data class EpisodeDetailUiState(
    val isLoading: Boolean = true,
    val episode: Episode? = null,
    val podcast: Podcast? = null,
)
