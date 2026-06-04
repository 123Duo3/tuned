package ink.duo3.tuned.data.work

import ink.duo3.tuned.core.AppError
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.repository.PodcastRepository

/**
 * Tallies a whole-library refresh into a schedulable [Summary]. The bounded fan-out and
 * per-feed isolation live in [PodcastRepository.refreshAll]; this class only classifies
 * the per-feed outcomes so WorkManager can decide whether the run is worth retrying.
 */
class FeedRefresher(
    private val podcastRepository: PodcastRepository,
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

    suspend fun refreshAll(): Summary {
        val results = podcastRepository.refreshAll()
        val succeeded = results.count { it is Outcome.Success }
        val retryable =
            results.count { it is Outcome.Failure && it.error.isRetryable() }
        return Summary(
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
        const val HTTP_REQUEST_TIMEOUT = 408
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_SERVER_ERROR = 500
    }
}
