package ink.duo3.tuned.data.repository

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.data.model.OpmlOutline
import ink.duo3.tuned.data.opml.OpmlParseException
import ink.duo3.tuned.data.opml.OpmlParser
import ink.duo3.tuned.domain.model.OpmlImportResult
import ink.duo3.tuned.domain.repository.OpmlRepository
import ink.duo3.tuned.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.first

/**
 * Bridges OPML documents and the [PodcastRepository] subscribe pipeline. Import
 * parses the document then subscribes feed-by-feed, isolating per-feed failures;
 * export reads the current subscriptions and serializes them via [OpmlParser].
 */
class OpmlRepositoryImpl(
    private val parser: OpmlParser,
    private val podcastRepository: PodcastRepository,
) : OpmlRepository {
    override suspend fun import(content: String): Outcome<OpmlImportResult> {
        val outlines =
            try {
                parser.parse(content.byteInputStream())
            } catch (e: OpmlParseException) {
                return Outcome.Failure(AppError.Parsing(e))
            }

        var imported = 0
        var failed = 0
        outlines.forEach { outline ->
            when (podcastRepository.subscribe(outline.xmlUrl)) {
                is Outcome.Success -> imported++
                is Outcome.Failure -> failed++
            }
        }
        return Outcome.Success(OpmlImportResult(imported = imported, failed = failed))
    }

    override suspend fun export(): Outcome<String> {
        val outlines =
            podcastRepository
                .observeSubscriptions()
                .first()
                .map { OpmlOutline(title = it.title, xmlUrl = it.feedUrl) }
        return Outcome.Success(parser.build(outlines))
    }
}
