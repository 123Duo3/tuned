package ink.duo3.tuned.data.repository

import ink.duo3.tuned.data.model.ItunesPodcastDto
import ink.duo3.tuned.domain.model.PodcastSearchResult

/**
 * Maps an iTunes podcast DTO (from search or lookup) into the domain [PodcastSearchResult],
 * preferring the larger artwork. Returns null for entries without a feed URL or title — they
 * can't be subscribed or rendered — so both the search and charts repositories drop them.
 * Shared so the two repositories map identically.
 */
@Suppress("ReturnCount")
internal fun ItunesPodcastDto.toPodcastSearchResult(): PodcastSearchResult? {
    val feed = feedUrl?.takeIf { it.isNotBlank() } ?: return null
    val name = collectionName?.takeIf { it.isNotBlank() } ?: return null
    return PodcastSearchResult(
        feedUrl = feed,
        title = name,
        author = artistName?.takeIf { it.isNotBlank() },
        artworkUrl = artworkUrl600 ?: artworkUrl100,
        episodeCount = trackCount,
    )
}
