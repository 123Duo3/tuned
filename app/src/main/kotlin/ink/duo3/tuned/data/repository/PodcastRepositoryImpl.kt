package ink.duo3.tuned.data.repository

import android.database.SQLException
import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.data.local.EpisodeMapper
import ink.duo3.tuned.data.local.FeedIdentity
import ink.duo3.tuned.data.local.TransactionRunner
import ink.duo3.tuned.data.local.asEpisodes
import ink.duo3.tuned.data.local.asPodcast
import ink.duo3.tuned.data.local.asPodcasts
import ink.duo3.tuned.data.local.dao.EpisodeDao
import ink.duo3.tuned.data.local.dao.PodcastDao
import ink.duo3.tuned.data.local.entity.PodcastEntity
import ink.duo3.tuned.data.model.ParsedFeed
import ink.duo3.tuned.data.network.FeedClient
import ink.duo3.tuned.data.network.FeedHttpException
import ink.duo3.tuned.data.network.FeedParseException
import ink.duo3.tuned.data.network.RssFeedParser
import ink.duo3.tuned.domain.repository.PodcastRepository
import java.io.IOException
import java.net.URI

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
    override fun observeSubscriptions() = podcastDao.observeAll().asPodcasts()

    override fun observePodcast(podcastId: String) = podcastDao.observeById(podcastId).asPodcast()

    override fun observeEpisodes(podcastId: String) = episodeDao.observeByPodcast(podcastId).asEpisodes()

    override suspend fun subscribe(feedUrl: String): Outcome<String> {
        // The search box accepts a bare host for convenience. Normalize before
        // validation so persistence and identity always see the URL actually fetched.
        val normalizedUrl = normalizeHttpUrl(feedUrl) ?: return Outcome.Failure(AppError.InvalidUrl())
        return runImport {
            val (fetched, feed) =
                resolveFeed(normalizedUrl)
                    // Unreachable without sending validators, but keeps the import idempotent.
                    ?: return@runImport Outcome.Success(FeedIdentity.podcastId(normalizedUrl))
            val id = FeedIdentity.podcastId(fetched.finalUrl)
            persist(id, canonicalFeedUrl = fetched.finalUrl, fetched = fetched, feed = feed)
            Outcome.Success(id)
        }
    }

    // A feed that has permanently moved advertises its canonical URL via
    // <itunes:new-feed-url>. Follow that chain at subscribe time so a redirect mirror
    // (e.g. a stale language/geo variant) never becomes the stored identity. Bounded to
    // avoid loops; a self-referential or cyclic pointer simply stops the walk.
    private suspend fun resolveFeed(startUrl: String): Pair<FeedClient.Fetched, ParsedFeed>? {
        val visited = mutableSetOf<String>()
        var url: String? = startUrl
        var resolved: Pair<FeedClient.Fetched, ParsedFeed>? = null
        while (url != null && visited.size <= MAX_NEW_FEED_HOPS) {
            val fetched = feedClient.fetch(url, etag = null, lastModified = null) as? FeedClient.Fetched ?: break
            val feed = parser.parse(fetched.body.inputStream())
            resolved = fetched to feed
            visited += fetched.finalUrl
            url = nextFeedUrl(fetched, feed.newFeedUrl, visited)
        }
        return resolved
    }

    private fun nextFeedUrl(
        fetched: FeedClient.Fetched,
        newFeedUrl: String?,
        visited: Set<String>,
    ): String? {
        val next = newFeedUrl?.let { normalizeHttpUrl(it) } ?: return null
        return next.takeIf { it != fetched.finalUrl && it !in visited }
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
                // Refresh deliberately does NOT follow new-feed-url: the identity was
                // fixed at subscribe time, and changing it here would orphan the row.
                is FeedClient.Fetched -> {
                    val feed = parser.parse(result.body.inputStream())
                    persist(podcastId, canonicalFeedUrl = podcast.canonicalFeedUrl, fetched = result, feed = feed)
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

    // java.net.URI runs identically on JVM and Android, so this validates the same in
    // tests and on device. Explicit non-http schemes are still rejected.
    private fun normalizeHttpUrl(raw: String): String? {
        val trimmed = raw.trim()
        val candidate =
            when {
                trimmed.isEmpty() -> return null
                SCHEME_PREFIX.matchesAt(trimmed, 0) -> trimmed
                trimmed.startsWith("//") -> "https:$trimmed"
                else -> "https://$trimmed"
            }
        return runCatching {
            val uri = URI(candidate)
            candidate.takeIf {
                uri.scheme?.lowercase() in HTTP_SCHEMES && !uri.host.isNullOrBlank()
            }
        }.getOrNull()
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
        const val MAX_NEW_FEED_HOPS = 5
        val HTTP_SCHEMES = setOf("http", "https")
        val SCHEME_PREFIX = Regex("[a-zA-Z][a-zA-Z0-9+.-]*://")
    }
}
