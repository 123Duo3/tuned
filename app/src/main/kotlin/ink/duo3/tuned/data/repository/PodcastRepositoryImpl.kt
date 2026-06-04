package ink.duo3.tuned.data.repository

import android.database.SQLException
import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.data.local.EpisodeMapper
import ink.duo3.tuned.data.local.FeedIdentity
import ink.duo3.tuned.data.local.TransactionRunner
import ink.duo3.tuned.data.local.asEpisode
import ink.duo3.tuned.data.local.asEpisodes
import ink.duo3.tuned.data.local.asPodcast
import ink.duo3.tuned.data.local.asPodcasts
import ink.duo3.tuned.data.local.asRecentEpisodes
import ink.duo3.tuned.data.local.dao.EpisodeDao
import ink.duo3.tuned.data.local.dao.PodcastDao
import ink.duo3.tuned.data.local.entity.PodcastEntity
import ink.duo3.tuned.data.model.ParsedFeed
import ink.duo3.tuned.data.network.FeedClient
import ink.duo3.tuned.data.network.FeedHttpException
import ink.duo3.tuned.data.network.FeedParseException
import ink.duo3.tuned.data.network.FeedResolver
import ink.duo3.tuned.domain.repository.PodcastRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.IOException

/**
 * The import pipeline: [FeedResolver] fetch/parse/discover → [EpisodeMapper] → Room.
 * Low-level failures are mapped into [AppError] so callers never see Ktor, Room, or
 * parser exceptions. [now] is injected so persistence timestamps stay deterministic
 * under test.
 */
class PodcastRepositoryImpl(
    private val resolver: FeedResolver,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val transaction: TransactionRunner,
    private val now: () -> Long,
) : PodcastRepository {
    override fun observeSubscriptions() = podcastDao.observeAll().asPodcasts()

    override fun observePodcast(podcastId: String) = podcastDao.observeById(podcastId).asPodcast()

    override fun observeEpisodes(podcastId: String) = episodeDao.observeByPodcast(podcastId).asEpisodes()

    override fun observeEpisode(episodeId: String) = episodeDao.observeById(episodeId).asEpisode()

    override fun observeRecentEpisodes(limit: Int) = episodeDao.observeRecent(limit).asRecentEpisodes()

    override suspend fun subscribe(feedUrl: String): Outcome<String> {
        // The search box accepts a bare host for convenience. Normalize before
        // validation so persistence and identity always see the URL actually fetched.
        val normalizedUrl = resolver.normalize(feedUrl) ?: return Outcome.Failure(AppError.InvalidUrl())
        return runImport {
            val (fetched, feed) =
                resolver.resolve(normalizedUrl)
                    // Unreachable without sending validators, but keeps the import idempotent.
                    ?: return@runImport Outcome.Success(FeedIdentity.podcastId(normalizedUrl))
            val id = FeedIdentity.podcastId(fetched.finalUrl)
            persist(id, canonicalFeedUrl = fetched.finalUrl, fetched = fetched, feed = feed)
            Outcome.Success(id)
        }
    }

    override suspend fun refresh(podcastId: String): Outcome<Unit> =
        runImport {
            val podcast =
                podcastDao.findById(podcastId)
                    ?: return@runImport Outcome.Failure(AppError.NotFound())
            when (val result = resolver.fetchConditional(podcast.currentFeedUrl, podcast.etag, podcast.lastModified)) {
                is FeedResolver.RefreshResult.Updated -> {
                    persist(
                        podcastId,
                        canonicalFeedUrl = podcast.canonicalFeedUrl,
                        fetched = result.fetched,
                        feed = result.feed,
                    )
                    Outcome.Success(Unit)
                }
                // 304 means the feed is unchanged but the check succeeded — record it
                // so refresh scheduling and library ordering see a fresh timestamp.
                FeedResolver.RefreshResult.NotModified -> {
                    podcastDao.upsert(podcast.copy(lastFetchedAt = now()))
                    Outcome.Success(Unit)
                }
            }
        }

    override suspend fun refreshAll(): List<Outcome<Unit>> =
        coroutineScope {
            // Snapshot the current library, then fan out under a permit gate so a large
            // subscription list can't open an unbounded number of sockets at once.
            val subscriptions = observeSubscriptions().first()
            val gate = Semaphore(REFRESH_CONCURRENCY)
            subscriptions
                .map { podcast -> async { gate.withPermit { refresh(podcast.id) } } }
                .map { it.await() }
        }

    private suspend fun persist(
        podcastId: String,
        canonicalFeedUrl: String,
        fetched: FeedClient.Fetched,
        feed: ParsedFeed,
    ) {
        // Map outside the transaction (CPU-only, no DB) so the lock is held for just the
        // two writes.
        val episodes = EpisodeMapper.map(podcastId, feed.items).episodes
        val podcast =
            PodcastEntity(
                id = podcastId,
                canonicalFeedUrl = canonicalFeedUrl,
                currentFeedUrl = fetched.finalUrl,
                etag = fetched.etag,
                lastModified = fetched.lastModified,
                lastFetchedAt = now(),
                title = feed.title,
                author = feed.author,
                description = feed.description,
                artworkUrl = feed.artworkUrl,
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

    private companion object {
        const val REFRESH_CONCURRENCY = 4
    }
}
