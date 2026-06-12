@file:Suppress("TooManyFunctions") // one entity↔domain mapper per table/projection

package ink.duo3.tuned.data.local

import ink.duo3.tuned.data.local.entity.EpisodeEntity
import ink.duo3.tuned.data.local.entity.PodcastEntity
import ink.duo3.tuned.data.local.entity.ProgressEntity
import ink.duo3.tuned.domain.model.Episode
import ink.duo3.tuned.domain.model.EpisodeProgress
import ink.duo3.tuned.domain.model.Podcast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ink.duo3.tuned.data.local.entity.RecentEpisodeView as REView
import ink.duo3.tuned.data.local.entity.SubscriptionLatestEpisodeView as SubView
import ink.duo3.tuned.domain.model.RecentEpisode as RE
import ink.duo3.tuned.domain.model.SubscriptionEpisode as SubEpisode

/** Flow adapters keep the repository's read surface a one-liner per query. */
internal fun Flow<List<PodcastEntity>>.asPodcasts(): Flow<List<Podcast>> = map { it.map(PodcastEntity::toDomain) }

internal fun Flow<PodcastEntity?>.asPodcast(): Flow<Podcast?> = map { it?.toDomain() }

internal fun Flow<List<EpisodeEntity>>.asEpisodes(): Flow<List<Episode>> = map { it.map(EpisodeEntity::toDomain) }

internal fun Flow<EpisodeEntity?>.asEpisode(): Flow<Episode?> = map { it?.toDomain() }

internal fun Flow<ProgressEntity?>.asProgress(): Flow<EpisodeProgress?> = map { it?.toDomain() }

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

internal fun ProgressEntity.toDomain(): EpisodeProgress =
    EpisodeProgress(
        episodeId = episodeId,
        positionMs = positionMs,
        completed = completed,
        lastPlayedAt = lastPlayedAt,
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
        chaptersUrl = chaptersUrl,
    )

internal fun Flow<List<REView>>.asRecentEpisodes(): Flow<List<RE>> = map { it.map(REView::toDomain) }

internal fun REView.toDomain(): RE =
    RE(
        id = id,
        podcastId = podcastId,
        title = title,
        description = description,
        artworkUrl = artworkUrl,
        publishedAtMs = publishedAt,
        durationMs = durationMs,
        podcastTitle = podcastTitle,
        podcastArtworkUrl = podcastArtworkUrl,
    )

internal fun Flow<List<SubView>>.asSubscriptionEpisodes(): Flow<List<SubEpisode>> = map { it.map(SubView::toDomain) }

internal fun SubView.toDomain(): SubEpisode =
    SubEpisode(
        podcastId = podcastId,
        podcastTitle = podcastTitle,
        podcastArtworkUrl = podcastArtworkUrl,
        episodeId = id,
        title = title,
        description = description,
        artworkUrl = artworkUrl,
        enclosureUrl = enclosureUrl,
        publishedAtMs = publishedAt,
        durationMs = durationMs,
    )
