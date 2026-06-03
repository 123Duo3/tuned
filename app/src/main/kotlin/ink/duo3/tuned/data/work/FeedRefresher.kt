package ink.duo3.tuned.data.work

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
        val failed: Int,
    ) {
        /** True only when there were feeds and every one failed — a likely transient blip. */
        val allFailed: Boolean get() = total > 0 && succeeded == 0
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
            Summary(total = results.size, succeeded = succeeded, failed = results.size - succeeded)
        }

    private companion object {
        const val DEFAULT_CONCURRENCY = 4
    }
}
