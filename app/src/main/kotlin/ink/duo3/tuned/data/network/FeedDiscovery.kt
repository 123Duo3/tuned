package ink.duo3.tuned.data.network

import java.net.URI

/**
 * Turns a non-feed URL into candidate feed URLs. Two strategies the importer tries in
 * order: RSS autodiscovery — the `<link rel="alternate" type="application/rss+xml">`
 * tags in a fetched HTML page — then a short list of conventional feed paths guessed
 * from the site's origin. Pure logic, no network; the caller fetches each candidate.
 */
object FeedDiscovery {
    /** Absolute feed URLs declared by `<link>` autodiscovery tags in [html]. */
    fun feedLinksIn(
        html: String,
        baseUrl: String,
    ): List<String> =
        LINK_TAG
            .findAll(html)
            .mapNotNull { feedHrefOrNull(it.value) }
            .mapNotNull { resolve(baseUrl, it) }
            .distinct()
            .toList()

    /** Conventional feed paths under [baseUrl]'s origin, e.g. `https://host/feed`. */
    fun guessPaths(baseUrl: String): List<String> {
        val origin = origin(baseUrl) ?: return emptyList()
        return COMMON_PATHS.map { origin + it }
    }

    private fun feedHrefOrNull(tag: String): String? {
        val attrs = ATTR.findAll(tag).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
        val rel = attrs["rel"].orEmpty().lowercase()
        val type = attrs["type"].orEmpty().lowercase()
        val isFeed =
            type.contains("rss") || type.contains("atom") || (rel.contains("alternate") && type.contains("xml"))
        return attrs["href"]?.takeIf { isFeed && it.isNotBlank() }
    }

    private fun resolve(
        baseUrl: String,
        href: String,
    ): String? = runCatching { URI(baseUrl).resolve(href.trim()).toString() }.getOrNull()

    private fun origin(baseUrl: String): String? =
        runCatching {
            val uri = URI(baseUrl)
            if (uri.scheme == null || uri.authority == null) {
                null
            } else {
                "${uri.scheme}://${uri.authority}"
            }
        }.getOrNull()

    private val LINK_TAG = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val ATTR = Regex("""(\w+)\s*=\s*["']([^"']*)["']""")
    private val COMMON_PATHS =
        listOf("/feed", "/feed.xml", "/rss", "/rss.xml", "/index.xml", "/atom.xml", "/podcast.xml")
}
