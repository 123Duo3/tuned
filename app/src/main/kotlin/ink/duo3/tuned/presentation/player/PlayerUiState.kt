package ink.duo3.tuned.presentation.player

import ink.duo3.tuned.domain.model.Chapter
import ink.duo3.tuned.domain.player.PlaybackState

/**
 * The full player's UI state: the raw [playback] snapshot plus the current episode's
 * [chapters] and which one is active. [currentChapterIndex] is null before the first
 * chapter's start (or when the episode has none).
 */
data class PlayerUiState(
    val playback: PlaybackState = PlaybackState(),
    val chapters: List<Chapter> = emptyList(),
    val currentChapterIndex: Int? = null,
) {
    val currentChapter: Chapter? get() = currentChapterIndex?.let(chapters::getOrNull)

    /** The active chapter's own art replaces the episode art while that chapter plays. */
    val artworkUrl: String? get() = currentChapter?.imageUrl ?: playback.artworkUrl
}
