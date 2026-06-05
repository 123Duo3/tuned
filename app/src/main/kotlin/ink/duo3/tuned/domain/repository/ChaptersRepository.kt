package ink.duo3.tuned.domain.repository

import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.model.Chapter

/**
 * Loads Podcasting 2.0 chapters from the JSON document an episode's `<podcast:chapters>`
 * tag points at. Embedded ID3 chapters take a different path (extracted by the Media3
 * player and surfaced through PlaybackController), so this interface covers only the
 * fetched-document source.
 */
interface ChaptersRepository {
    /**
     * Returns the chapters declared at [chaptersUrl], sorted by start time. Implementations
     * cache by URL so reopening the same episode doesn't re-fetch. A successful fetch of a
     * document with no usable chapters is an empty list, not a failure.
     */
    suspend fun chapters(chaptersUrl: String): Outcome<List<Chapter>>
}
