package ink.duo3.tuned.domain.repository

import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.model.Episode
import ink.duo3.tuned.domain.model.Podcast
import kotlinx.coroutines.flow.Flow

/**
 * Subscription + feed-sync surface for the rest of the app. Implementations own the
 * fetch → parse → persist pipeline; callers see only [Outcome] with a typed error.
 */
interface PodcastRepository {
    /** Library feed: all subscriptions, newest-refreshed first. Emits on every change. */
    fun observeSubscriptions(): Flow<List<Podcast>>

    /** A podcast's episodes, newest-published first. Emits on every change. */
    fun observeEpisodes(podcastId: String): Flow<List<Episode>>

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
}
