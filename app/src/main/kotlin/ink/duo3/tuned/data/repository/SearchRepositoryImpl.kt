package ink.duo3.tuned.data.repository

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.data.network.FeedHttpException
import ink.duo3.tuned.data.network.ItunesSearchApi
import ink.duo3.tuned.domain.model.PodcastSearchResult
import ink.duo3.tuned.domain.repository.SearchRepository
import kotlinx.serialization.SerializationException
import java.io.IOException

/**
 * iTunes-backed [SearchRepository]. Maps the wire DTO into the domain result, dropping
 * entries without a feed URL or title (they can't be subscribed or rendered) and any
 * duplicate feed URLs (iTunes occasionally repeats one, which would collide as a list
 * key), and folds Ktor/serialization failures into [AppError] so presentation never
 * sees them.
 */
class SearchRepositoryImpl(
    private val api: ItunesSearchApi,
) : SearchRepository {
    override suspend fun searchPodcasts(term: String): Outcome<List<PodcastSearchResult>> =
        try {
            Outcome.Success(
                api
                    .search(term)
                    .results
                    .mapNotNull { it.toPodcastSearchResult() }
                    .distinctBy { it.feedUrl },
            )
        } catch (e: FeedHttpException) {
            Outcome.Failure(AppError.Http(e.code, e))
        } catch (e: SerializationException) {
            Outcome.Failure(AppError.Parsing(e))
        } catch (e: IOException) {
            Outcome.Failure(AppError.Network(e))
        }
}
