package ink.duo3.tuned.data.repository

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.data.network.FeedHttpException
import ink.duo3.tuned.data.network.ItunesChartsApi
import ink.duo3.tuned.data.network.ItunesSearchApi
import ink.duo3.tuned.domain.model.PodcastSearchResult
import ink.duo3.tuned.domain.repository.ChartsRepository
import kotlinx.serialization.SerializationException
import java.io.IOException

/**
 * iTunes-backed [ChartsRepository]. The charts endpoint yields only ranked collection ids, so
 * a second [ItunesSearchApi.lookup] resolves the feed URLs; lookup doesn't preserve order, so
 * results are re-keyed by collection id and emitted in the original chart rank. Entries without
 * a feed URL/title are dropped, duplicate feeds collapsed, and Ktor/serialization failures
 * folded into [AppError] so presentation never sees them.
 */
class ChartsRepositoryImpl(
    private val chartsApi: ItunesChartsApi,
    private val searchApi: ItunesSearchApi,
) : ChartsRepository {
    override suspend fun topPodcasts(
        country: String,
        genreId: Int?,
    ): Outcome<List<PodcastSearchResult>> =
        try {
            val ids = chartsApi.topPodcastIds(country, genreId = genreId)
            Outcome.Success(resolve(ids, country))
        } catch (e: FeedHttpException) {
            Outcome.Failure(AppError.Http(e.code, e))
        } catch (e: SerializationException) {
            Outcome.Failure(AppError.Parsing(e))
        } catch (e: IOException) {
            Outcome.Failure(AppError.Network(e))
        }

    private suspend fun resolve(
        ids: List<String>,
        country: String,
    ): List<PodcastSearchResult> {
        if (ids.isEmpty()) return emptyList()
        val byId =
            searchApi
                .lookup(ids, country)
                .results
                .mapNotNull { dto -> dto.collectionId?.let { it.toString() to dto } }
                .toMap()
        return ids
            .mapNotNull { byId[it]?.toPodcastSearchResult() }
            .distinctBy { it.feedUrl }
    }
}
