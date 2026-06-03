package ink.duo3.tuned.domain.repository

import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.model.OpmlImportResult

/**
 * OPML in/out — the "no lock-in" guarantee. Import reuses the subscribe pipeline;
 * export serializes the current subscriptions. The UI depends only on this interface.
 */
interface OpmlRepository {
    /**
     * Parses [content] as an OPML subscription list and subscribes to each feed.
     * A parse failure resolves to [Outcome.Failure]; per-feed subscribe failures are
     * counted into [OpmlImportResult.failed] rather than aborting the whole import.
     */
    suspend fun import(content: String): Outcome<OpmlImportResult>

    /** Serializes the current subscriptions into an OPML 2.0 document. */
    suspend fun export(): Outcome<String>
}
