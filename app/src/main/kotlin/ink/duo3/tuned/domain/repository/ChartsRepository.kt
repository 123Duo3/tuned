package ink.duo3.tuned.domain.repository

import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.model.PodcastSearchResult

/**
 * Top-charts discovery, kept behind an interface so the source (Apple/iTunes charts for MVP)
 * can later be swapped for Podcast Index trending without touching presentation. Results reuse
 * [PodcastSearchResult] so the home strip and search list render and subscribe identically.
 * Implementations map transport/parse failures into a typed [ink.duo3.tuned.core.AppError].
 */
interface ChartsRepository {
    /**
     * Top podcasts for [country] (ISO 3166-1 alpha-2), optionally narrowed to an iTunes
     * [genreId]. Results carry a usable RSS feed URL and preserve the chart's ranking order.
     */
    suspend fun topPodcasts(
        country: String,
        genreId: Int? = null,
    ): Outcome<List<PodcastSearchResult>>
}
