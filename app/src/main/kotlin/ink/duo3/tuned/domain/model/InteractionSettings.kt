package ink.duo3.tuned.domain.model

/**
 * User interaction and display preferences. [usePreciseTime] switches timestamps from relative
 * ("3 hours ago", "Yesterday") to precise dates and clocks.
 */
data class InteractionSettings(
    val hapticFeedbackEnabled: Boolean = true,
    val usePreciseTime: Boolean = false,
)
