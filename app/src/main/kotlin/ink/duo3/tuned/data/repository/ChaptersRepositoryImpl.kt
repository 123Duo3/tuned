package ink.duo3.tuned.data.repository

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.data.model.ChapterDto
import ink.duo3.tuned.data.network.ChaptersApi
import ink.duo3.tuned.data.network.FeedHttpException
import ink.duo3.tuned.domain.model.Chapter
import ink.duo3.tuned.domain.repository.ChaptersRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import java.io.IOException

/**
 * Fetches Podcasting 2.0 chapters via [ChaptersApi], maps the wire DTO into domain
 * [Chapter]s — dropping entries with no start time (unplaceable) or with `toc == false`
 * (image cues not meant for the chapter list), sorting by start time — and folds
 * Ktor/serialization failures into [AppError]. Successful results are cached by URL in a
 * bounded LRU so reopening an episode doesn't re-fetch; only successes are cached, so a
 * transient failure can be retried.
 */
class ChaptersRepositoryImpl(
    private val api: ChaptersApi,
    private val cacheCapacity: Int = DEFAULT_CACHE_CAPACITY,
) : ChaptersRepository {
    private val mutex = Mutex()
    private val cache =
        object : LinkedHashMap<String, List<Chapter>>(0, LOAD_FACTOR, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, List<Chapter>>): Boolean = size > cacheCapacity
        }

    override suspend fun chapters(chaptersUrl: String): Outcome<List<Chapter>> {
        mutex.withLock { cache[chaptersUrl] }?.let { return Outcome.Success(it) }
        return try {
            val chapters =
                api
                    .fetch(chaptersUrl)
                    .chapters
                    .filter { it.toc != false }
                    .mapNotNull { it.toChapter() }
                    .sortedBy { it.startTimeMs }
            mutex.withLock { cache[chaptersUrl] = chapters }
            Outcome.Success(chapters)
        } catch (e: FeedHttpException) {
            Outcome.Failure(AppError.Http(e.code, e))
        } catch (e: SerializationException) {
            Outcome.Failure(AppError.Parsing(e))
        } catch (e: IOException) {
            Outcome.Failure(AppError.Network(e))
        }
    }

    private companion object {
        const val DEFAULT_CACHE_CAPACITY = 64
        const val LOAD_FACTOR = 0.75f
        const val MILLIS_PER_SECOND = 1000.0
    }

    private fun ChapterDto.toChapter(): Chapter? {
        val start = startTime ?: return null
        return Chapter(
            startTimeMs = (start * MILLIS_PER_SECOND).toLong(),
            title = title?.trim()?.ifBlank { null },
            imageUrl = img?.trim()?.ifBlank { null },
            url = url?.trim()?.ifBlank { null },
        )
    }
}
