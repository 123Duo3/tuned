package ink.duo3.tuned.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * The daily background refresh entry point. WorkManager's default factory constructs
 * this via the (Context, WorkerParameters) constructor; the [FeedRefresher] is pulled
 * from the running Koin container instead of a custom WorkerFactory.
 */
class FeedRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {
    private val refresher: FeedRefresher by inject()

    override suspend fun doWork(): Result {
        val summary = refresher.refreshAll()
        // Individual broken feeds are expected and never fail the run. Only retry when the
        // whole run failed *and* every failure was transient; a wipeout of permanent errors
        // (404, parse failure) should wait for the next scheduled run, not back off in place.
        return if (summary.shouldRetry) Result.retry() else Result.success()
    }
}
