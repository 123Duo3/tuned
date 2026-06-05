package ink.duo3.tuned.domain.model

/**
 * User interaction preferences shared by gesture, confirmation, and playback feedback.
 */
data class InteractionSettings(
    val hapticFeedbackEnabled: Boolean = true,
)
