package ink.duo3.tuned.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Enqueues the once-a-day feed refresh. [ExistingPeriodicWorkPolicy.KEEP] means the
 * schedule survives app restarts without being reset on every launch, and the network
 * constraint defers the run until the device is online.
 */
class FeedRefreshScheduler(
    private val context: Context,
) {
    fun schedule() {
        val request =
            PeriodicWorkRequestBuilder<FeedRefreshWorker>(REFRESH_INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(
                    Constraints
                        .Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private companion object {
        const val WORK_NAME = "feed-refresh"
        const val REFRESH_INTERVAL_HOURS = 24L
    }
}
