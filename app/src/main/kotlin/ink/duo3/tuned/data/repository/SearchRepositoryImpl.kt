package ink.duo3.tuned.data.repository

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.data.model.ItunesPodcastDto
import ink.duo3.tuned.data.network.FeedHttpException
import ink.duo3.tuned.data.network.ItunesSearchApi
import ink.duo3.tuned.domain.model.PodcastSearchResult
import ink.duo3.tuned.domain.repository.SearchRepository
import kotlinx.serialization.SerializationException
import java.io.IOException

/**
 * iTunes-backed [SearchRepository]. Maps the wire DTO into the domain result, dropping
 * entries without a feed URL or title (they can't be subscribed or rendered), and folds
 * Ktor/serialization failures into [AppError] so presentation never sees them.
 */
class SearchRepositoryImpl(
    private val api: ItunesSearchApi,
) : SearchRepository {
    override suspend fun searchPodcasts(term: String): Outcome<List<PodcastSearchResult>> =
        try {
            Outcome.Success(api.search(term).results.mapNotNull(::toResult))
        } catch (e: FeedHttpException) {
            Outcome.Failure(AppError.Http(e.code, e))
        } catch (e: SerializationException) {
            Outcome.Failure(AppError.Parsing(e))
        } catch (e: IOException) {
            Outcome.Failure(AppError.Network(e))
        }

    private fun toResult(dto: ItunesPodcastDto): PodcastSearchResult? {
        val feedUrl = dto.feedUrl?.takeIf { it.isNotBlank() }
        val title = dto.collectionName?.takeIf { it.isNotBlank() }
        if (feedUrl == null || title == null) return null
        return PodcastSearchResult(
            feedUrl = feedUrl,
            title = title,
            author = dto.artistName?.takeIf { it.isNotBlank() },
            artworkUrl = dto.artworkUrl600 ?: dto.artworkUrl100,
            episodeCount = dto.trackCount,
        )
    }
}
