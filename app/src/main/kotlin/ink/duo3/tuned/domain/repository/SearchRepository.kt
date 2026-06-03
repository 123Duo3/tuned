package ink.duo3.tuned.domain.repository

import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.model.PodcastSearchResult

/**
 * Podcast discovery by keyword, kept behind an interface so the source (iTunes Search for
 * MVP) can later be swapped for Podcast Index without touching presentation. Implementations
 * map transport/parse failures into a typed [ink.duo3.tuned.core.AppError].
 */
interface SearchRepository {
    /** Searches the catalog for [term]; results always carry a usable RSS feed URL. */
    suspend fun searchPodcasts(term: String): Outcome<List<PodcastSearchResult>>
}
