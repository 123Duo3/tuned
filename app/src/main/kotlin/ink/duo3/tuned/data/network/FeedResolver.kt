package ink.duo3.tuned.data.network

import ink.duo3.tuned.data.model.ParsedFeed
import java.io.IOException
import java.net.URI

/**
 * Turns a user-supplied URL into the feed actually worth subscribing to. It fetches the
 * input, and:
 *  - if it parses as RSS, follows any `<itunes:new-feed-url>` chain so a redirect mirror
 *    (a stale language/geo variant) never becomes the stored identity;
 *  - if it doesn't (the user pasted a site URL), discovers the feed via [FeedDiscovery]
 *    — autodiscovery `<link>` tags first, then conventional feed paths.
 *
 * Network and parse exceptions for *discovery candidates* are swallowed (a 404 or an
 * HTML page is just a miss); failures fetching the user's actual input still propagate
 * to the repository's error mapping.
 */
class FeedResolver(
    private val feedClient: FeedClient,
    private val parser: RssFeedParser,
) {
    /** Normalize a raw input to an http(s) URL, or null if it isn't one. */
    fun normalize(raw: String): String? {
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
            candidate.takeIf { uri.scheme?.lowercase() in HTTP_SCHEMES && !uri.host.isNullOrBlank() }
        }.getOrNull()
    }

    sealed interface RefreshResult {
        class Updated(
            val fetched: FeedClient.Fetched,
            val feed: ParsedFeed,
        ) : RefreshResult

        data object NotModified : RefreshResult
    }

    /**
     * Refresh path for a known feed URL: a conditional fetch, parsing the body when the
     * server returns content. Unlike [resolve] this does not discover or follow
     * new-feed-url — refresh must keep the identity fixed at subscribe time. A malformed
     * body propagates as [FeedParseException].
     */
    suspend fun fetchConditional(
        url: String,
        etag: String?,
        lastModified: String?,
    ): RefreshResult =
        when (val result = feedClient.fetch(url, etag, lastModified)) {
            is FeedClient.Fetched -> RefreshResult.Updated(result, parser.parse(result.body.inputStream()))
            FeedClient.NotModified -> RefreshResult.NotModified
        }

    /**
     * null only when the first fetch is a 304, which can't happen without validators.
     * Throws [FeedParseException] when the input is reachable but no feed can be found.
     */
    suspend fun resolve(startUrl: String): Pair<FeedClient.Fetched, ParsedFeed>? {
        val fetched = feedClient.fetch(startUrl, etag = null, lastModified = null) as? FeedClient.Fetched ?: return null
        return parseOrNull(fetched.body)
            ?.let { follow(fetched, it) }
            ?: discover(fetched, startUrl)
    }

    // Bounded walk of the new-feed-url chain; a self-referential, cyclic, or non-feed
    // pointer simply stops it.
    private suspend fun follow(
        first: FeedClient.Fetched,
        firstFeed: ParsedFeed,
    ): Pair<FeedClient.Fetched, ParsedFeed> {
        val visited = mutableSetOf(first.finalUrl)
        var fetched = first
        var feed = firstFeed
        var next = nextFeedUrl(fetched, feed.newFeedUrl, visited)
        while (next != null && visited.size <= MAX_NEW_FEED_HOPS) {
            val hop = fetchAndParse(next) ?: break
            fetched = hop.first
            feed = hop.second
            visited += fetched.finalUrl
            next = nextFeedUrl(fetched, feed.newFeedUrl, visited)
        }
        return fetched to feed
    }

    private suspend fun discover(
        page: FeedClient.Fetched,
        startUrl: String,
    ): Pair<FeedClient.Fetched, ParsedFeed> {
        val candidates =
            (FeedDiscovery.feedLinksIn(page.body.decodeToString(), page.finalUrl) + FeedDiscovery.guessPaths(startUrl))
                .distinct()
                .filter { it != page.finalUrl }
        return candidates.firstNotNullOfOrNull { candidate ->
            fetchAndParse(candidate)?.let { follow(it.first, it.second) }
        } ?: throw FeedParseException("No feed found at $startUrl or its common locations", null)
    }

    @Suppress("SwallowedException") // a candidate that errors or isn't a feed is a miss, not a failure
    private suspend fun fetchAndParse(url: String): Pair<FeedClient.Fetched, ParsedFeed>? =
        try {
            (feedClient.fetch(url, etag = null, lastModified = null) as? FeedClient.Fetched)
                ?.let { fetched -> parseOrNull(fetched.body)?.let { fetched to it } }
        } catch (e: FeedHttpException) {
            null
        } catch (e: IOException) {
            null
        }

    @Suppress("SwallowedException") // "not a feed" is recoverable here; a malformed known feed is surfaced elsewhere
    private fun parseOrNull(body: ByteArray): ParsedFeed? =
        try {
            parser.parse(body.inputStream())
        } catch (e: FeedParseException) {
            null
        }

    private fun nextFeedUrl(
        fetched: FeedClient.Fetched,
        newFeedUrl: String?,
        visited: Set<String>,
    ): String? {
        val next = newFeedUrl?.let { normalize(it) } ?: return null
        return next.takeIf { it != fetched.finalUrl && it !in visited }
    }

    private companion object {
        const val MAX_NEW_FEED_HOPS = 5
        val HTTP_SCHEMES = setOf("http", "https")
        val SCHEME_PREFIX = Regex("[a-zA-Z][a-zA-Z0-9+.-]*://")
    }
}
