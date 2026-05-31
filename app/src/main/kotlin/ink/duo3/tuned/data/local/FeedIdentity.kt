package ink.duo3.tuned.data.local

import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest

/**
 * Stable primary-key derivation for podcasts and episodes. Pure and deterministic
 * so dedup behaviour is testable without a database or network.
 *
 * Identity rules (see CLAUDE.md):
 *  - Podcast id  = SHA-256 of the normalized canonical feed URL.
 *  - Episode id  = SHA-256 of `podcastId` + the first non-blank of:
 *                  feed `guid` → `enclosureUrl` → `title` + `publishedAt`.
 *                  Returns null when none of those exist; the import pipeline must
 *                  skip such items and log the reason rather than collapse them all
 *                  onto one degenerate id.
 *
 * Scheme canonicalization (http vs https) is the import pipeline's job: by the time
 * an id is computed, [podcastId] is fed the redirect-resolved canonical URL.
 */
object FeedIdentity {
    fun podcastId(canonicalFeedUrl: String): String = sha256(normalizeUrl(canonicalFeedUrl))

    fun episodeId(
        podcastId: String,
        guid: String?,
        enclosureUrl: String?,
        title: String?,
        publishedAtMs: Long?,
    ): String? {
        val trimmedTitle = title?.trim().orEmpty()
        val key =
            guid?.takeIf { it.isNotBlank() }
                ?: enclosureUrl?.takeIf { it.isNotBlank() }
                ?: if (trimmedTitle.isEmpty() && publishedAtMs == null) {
                    return null
                } else {
                    "$trimmedTitle\n${publishedAtMs ?: 0L}"
                }
        return sha256("$podcastId\n$key")
    }

    /**
     * Lowercases only scheme and host (both case-insensitive per RFC 3986) and strips
     * a trailing slash from the path. Path and query case/encoding are preserved, so
     * `?token=AbC` and `?token=abc` stay distinct. Unparseable input falls back to a
     * trailing-slash trim.
     */
    internal fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return try {
            val uri = URI(trimmed)
            val scheme = uri.scheme?.lowercase()
            val host = uri.host?.lowercase()
            if (scheme == null || host == null) {
                trimmed.trimEnd('/')
            } else {
                val port = if (uri.port == -1) "" else ":${uri.port}"
                val path = uri.rawPath.orEmpty().trimEnd('/')
                val query = uri.rawQuery?.let { "?$it" }.orEmpty()
                "$scheme://$host$port$path$query"
            }
        } catch (_: URISyntaxException) {
            trimmed.trimEnd('/')
        }
    }

    private fun sha256(value: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
