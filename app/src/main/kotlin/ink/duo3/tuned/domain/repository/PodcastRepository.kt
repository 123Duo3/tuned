package ink.duo3.tuned.domain.repository

import ink.duo3.tuned.core.Outcome

/**
 * Subscription + feed-sync surface for the rest of the app. Implementations own the
 * fetch → parse → persist pipeline; callers see only [Outcome] with a typed error.
 */
interface PodcastRepository {
    /**
     * Imports the feed at [feedUrl] (following redirects) and stores the podcast plus
     * its episodes. Returns the stable podcast id derived from the resolved feed URL.
     */
    suspend fun subscribe(feedUrl: String): Outcome<String>

    /**
     * Re-fetches a known podcast using its stored `ETag`/`Last-Modified` validators and
     * upserts any changes. A `304 Not Modified` resolves to success with no writes.
     */
    suspend fun refresh(podcastId: String): Outcome<Unit>
}
