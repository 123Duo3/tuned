package ink.duo3.tuned.data.work

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.repository.PodcastRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Refreshes every subscription with bounded concurrency. Per the reliability rule a
 * single broken feed must never fail the whole run, so per-feed failures are counted
 * into [Summary] rather than thrown. Conditional `ETag`/`Last-Modified` requests live
 * inside [PodcastRepository.refresh]; this class only fans out and tallies.
 */
class FeedRefresher(
    private val podcastRepository: PodcastRepository,
    private val maxConcurrency: Int = DEFAULT_CONCURRENCY,
) {
    data class Summary(
        val total: Int,
        val succeeded: Int,
        val retryableFailures: Int,
        val permanentFailures: Int,
    ) {
        val failed: Int get() = retryableFailures + permanentFailures

        /**
         * True only when every feed failed *and* every failure was transient (network /
         * 5xx / throttling). A whole-run wipeout of permanent errors (404, parse failure,
         * dead feed) must not trigger a backoff retry — it would just burn battery until
         * the next scheduled run; only transient wipeouts are worth retrying sooner.
         */
        val shouldRetry: Boolean get() = total > 0 && succeeded == 0 && permanentFailures == 0
    }

    suspend fun refreshAll(): Summary =
        coroutineScope {
            val subscriptions = podcastRepository.observeSubscriptions().first()
            val gate = Semaphore(maxConcurrency)
            val results =
                subscriptions
                    .map { podcast ->
                        async { gate.withPermit { podcastRepository.refresh(podcast.id) } }
                    }.map { it.await() }
            val succeeded = results.count { it is Outcome.Success }
            val retryable =
                results.count { it is Outcome.Failure && it.error.isRetryable() }
            Summary(
                total = results.size,
                succeeded = succeeded,
                retryableFailures = retryable,
                permanentFailures = results.size - succeeded - retryable,
            )
        }

    private fun AppError.isRetryable(): Boolean =
        when (this) {
            is AppError.Network -> true
            // Server-side and throttling responses may clear up shortly; client errors won't.
            is AppError.Http ->
                code == HTTP_REQUEST_TIMEOUT ||
                    code == HTTP_TOO_MANY_REQUESTS ||
                    code >= HTTP_SERVER_ERROR
            else -> false
        }

    private companion object {
        const val DEFAULT_CONCURRENCY = 4
        const val HTTP_REQUEST_TIMEOUT = 408
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_SERVER_ERROR = 500
    }
}
