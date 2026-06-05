package ink.duo3.tuned.domain.repository

import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.model.Chapter
import ink.duo3.tuned.domain.model.Episode

/**
 * Resolves an episode's chapters from the best available source. Implementations prefer the
 * Podcasting 2.0 JSON document the feed declares, then fall back to chapters embedded in the
 * audio file's ID3 tag. Best-effort: an episode with no chapters yields an empty list.
 */
interface ChaptersRepository {
    /** Chapters for [episode], sorted by start time; empty when the episode declares none. */
    suspend fun chapters(episode: Episode): Outcome<List<Chapter>>
}
