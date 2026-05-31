package ink.duo3.tuned.data.repository

import android.database.SQLException
import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.data.local.EpisodeMapper
import ink.duo3.tuned.data.local.FeedIdentity
import ink.duo3.tuned.data.local.TransactionRunner
import ink.duo3.tuned.data.local.dao.EpisodeDao
import ink.duo3.tuned.data.local.dao.PodcastDao
import ink.duo3.tuned.data.local.entity.PodcastEntity
import ink.duo3.tuned.data.network.FeedClient
import ink.duo3.tuned.data.network.FeedHttpException
import ink.duo3.tuned.data.network.FeedParseException
import ink.duo3.tuned.data.network.RssFeedParser
import ink.duo3.tuned.domain.repository.PodcastRepository
import java.io.IOException

/**
 * The import pipeline: [FeedClient] fetch → [RssFeedParser] parse → [EpisodeMapper]
 * → Room. Low-level failures are mapped into [AppError] so callers never see Ktor,
 * Room, or parser exceptions. [now] is injected so persistence timestamps stay
 * deterministic under test.
 */
class PodcastRepositoryImpl(
    private val feedClient: FeedClient,
    private val parser: RssFeedParser,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val transaction: TransactionRunner,
    private val now: () -> Long,
) : PodcastRepository {
    override suspend fun subscribe(feedUrl: String): Outcome<String> =
        runImport {
            when (val result = feedClient.fetch(feedUrl, etag = null, lastModified = null)) {
                is FeedClient.Fetched -> {
                    val id = FeedIdentity.podcastId(result.finalUrl)
                    persist(id, canonicalFeedUrl = result.finalUrl, fetched = result)
                    Outcome.Success(id)
                }
                // Unreachable without sending validators, but keeps the import idempotent.
                FeedClient.NotModified -> Outcome.Success(FeedIdentity.podcastId(feedUrl))
            }
        }

    override suspend fun refresh(podcastId: String): Outcome<Unit> =
        runImport {
            val podcast =
                podcastDao.findById(podcastId)
                    ?: return@runImport Outcome.Failure(AppError.NotFound())
            when (
                val result =
                    feedClient.fetch(podcast.currentFeedUrl, podcast.etag, podcast.lastModified)
            ) {
                is FeedClient.Fetched -> {
                    persist(podcastId, canonicalFeedUrl = podcast.canonicalFeedUrl, fetched = result)
                    Outcome.Success(Unit)
                }
                // 304 means the feed is unchanged but the check succeeded — record it
                // so refresh scheduling and library ordering see a fresh timestamp.
                FeedClient.NotModified -> {
                    podcastDao.upsert(podcast.copy(lastFetchedAt = now()))
                    Outcome.Success(Unit)
                }
            }
        }

    private suspend fun persist(
        podcastId: String,
        canonicalFeedUrl: String,
        fetched: FeedClient.Fetched,
    ) {
        // Parse + map outside the transaction (CPU-only, no DB) so the lock is held
        // for just the two writes.
        val feed = parser.parse(fetched.body.inputStream())
        val episodes = EpisodeMapper.map(podcastId, feed.items).episodes
        val podcast =
            PodcastEntity(
                id = podcastId,
                canonicalFeedUrl = canonicalFeedUrl,
                currentFeedUrl = fetched.finalUrl,
                etag = fetched.etag,
                lastModified = fetched.lastModified,
                lastFetchedAt = now(),
            )
        // Atomic: a crash between the two writes must not persist new validators while
        // leaving episodes stale, or the next 304 would freeze the stale state.
        transaction {
            podcastDao.upsert(podcast)
            episodeDao.upsertAll(episodes)
        }
    }

    private inline fun <T> runImport(block: () -> Outcome<T>): Outcome<T> =
        try {
            block()
        } catch (e: FeedHttpException) {
            Outcome.Failure(AppError.Http(e.code, e))
        } catch (e: FeedParseException) {
            Outcome.Failure(AppError.Parsing(e))
        } catch (e: SQLException) {
            Outcome.Failure(AppError.Storage(e))
        } catch (e: IOException) {
            Outcome.Failure(AppError.Network(e))
        }
}
