package ink.duo3.tuned.domain.repository

import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.model.Episode
import ink.duo3.tuned.domain.model.Podcast
import ink.duo3.tuned.domain.model.RecentEpisode
import kotlinx.coroutines.flow.Flow

/**
 * Subscription + feed-sync surface for the rest of the app. Implementations own the
 * fetch → parse → persist pipeline; callers see only [Outcome] with a typed error.
 */
interface PodcastRepository {
    /** Library feed: all subscriptions, newest-refreshed first. Emits on every change. */
    fun observeSubscriptions(): Flow<List<Podcast>>

    /** A single podcast's metadata, or null if it is not subscribed. Emits on every change. */
    fun observePodcast(podcastId: String): Flow<Podcast?>

    /** A podcast's episodes, newest-published first. Emits on every change. */
    fun observeEpisodes(podcastId: String): Flow<List<Episode>>

    /** A single episode, or null if it is not stored. Emits on every change. */
    fun observeEpisode(episodeId: String): Flow<Episode?>

    /** The latest episodes across all subscriptions, newest-published first. */
    fun observeRecentEpisodes(limit: Int = 30): Flow<List<RecentEpisode>>

    /**
     * Imports the feed at [feedUrl] (following redirects) and stores the podcast plus
     * its episodes. Returns the stable podcast id derived from the resolved feed URL.
     */
    suspend fun subscribe(feedUrl: String): Outcome<String>

    /**
     * Re-fetches a known podcast using its stored `ETag`/`Last-Modified` validators and
     * upserts any changes. A `304 Not Modified` still resolves to success and bumps the
     * podcast's `lastFetchedAt` so refresh scheduling sees a fresh check.
     */
    suspend fun refresh(podcastId: String): Outcome<Unit>

    /**
     * Refreshes every current subscription with bounded concurrency, returning one
     * [Outcome] per feed in subscription order. Per the reliability rule a single broken
     * feed must not fail the whole run, so failures are reported per-feed rather than
     * thrown. An empty library yields an empty list.
     */
    suspend fun refreshAll(): List<Outcome<Unit>>
}
