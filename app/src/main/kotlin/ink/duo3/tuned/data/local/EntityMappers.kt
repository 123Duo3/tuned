package ink.duo3.tuned.data.local

import ink.duo3.tuned.data.local.entity.EpisodeEntity
import ink.duo3.tuned.data.local.entity.PodcastEntity
import ink.duo3.tuned.domain.model.Episode
import ink.duo3.tuned.domain.model.Podcast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Flow adapters keep the repository's read surface a one-liner per query. */
internal fun Flow<List<PodcastEntity>>.asPodcasts(): Flow<List<Podcast>> = map { it.map(PodcastEntity::toDomain) }

internal fun Flow<PodcastEntity?>.asPodcast(): Flow<Podcast?> = map { it?.toDomain() }

internal fun Flow<List<EpisodeEntity>>.asEpisodes(): Flow<List<Episode>> = map { it.map(EpisodeEntity::toDomain) }

internal fun Flow<EpisodeEntity?>.asEpisode(): Flow<Episode?> = map { it?.toDomain() }

/** Room entity -> domain model. Keeps Room types out of the read surface. */
internal fun PodcastEntity.toDomain(): Podcast =
    Podcast(
        id = id,
        feedUrl = currentFeedUrl,
        title = title,
        author = author,
        description = description,
        artworkUrl = artworkUrl,
    )

internal fun EpisodeEntity.toDomain(): Episode =
    Episode(
        id = id,
        podcastId = podcastId,
        title = title,
        description = description,
        enclosureUrl = enclosureUrl,
        artworkUrl = artworkUrl,
        publishedAtMs = publishedAt,
        durationMs = durationMs,
    )
