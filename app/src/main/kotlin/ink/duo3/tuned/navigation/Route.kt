package ink.duo3.tuned.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes (no string routes). Cross-feature navigation goes
 * through these — feature packages never reference each other directly.
 */
sealed interface Route : NavKey {
    @Serializable
    data object Home : Route

    @Serializable
    data object Search : Route

    @Serializable
    data object Library : Route

    @Serializable
    data class PodcastDetail(
        val podcastId: String,
    ) : Route

    @Serializable
    data class EpisodeDetail(
        val episodeId: String,
    ) : Route

    @Serializable
    data object Player : Route
}
