package ink.duo3.tuned.data.model

/**
 * One feed entry parsed from (or written to) an OPML document. Data-layer-internal:
 * the importer maps these onto the subscribe pipeline and the exporter builds them
 * from stored subscriptions. [title] is best-effort; [xmlUrl] is the feed address.
 */
data class OpmlOutline(
    val title: String?,
    val xmlUrl: String,
)
