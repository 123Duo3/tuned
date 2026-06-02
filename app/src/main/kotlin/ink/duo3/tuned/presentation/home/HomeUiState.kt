package ink.duo3.tuned.presentation.home

import ink.duo3.tuned.domain.model.Podcast

/**
 * State for the card-based home. [isLoading] covers the first DB read; [subscriptions]
 * backs the "Subscribed" card's artwork row; [isPlaying] drives the wordmark's baseline
 * animation while audio plays. More section data (recently updated, resume) joins this
 * state as those cards land.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val subscriptions: List<Podcast> = emptyList(),
    val isPlaying: Boolean = false,
)
