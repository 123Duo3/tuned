package ink.duo3.tuned.domain.model

/**
 * Outcome of importing an OPML subscription list. [imported] feeds subscribed
 * successfully; [failed] feeds were parsed but could not be subscribed (network,
 * malformed feed, …). A parse-level failure surfaces as a repository error instead.
 */
data class OpmlImportResult(
    val imported: Int,
    val failed: Int,
) {
    val total: Int get() = imported + failed
}
