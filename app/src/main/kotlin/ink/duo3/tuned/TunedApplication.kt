package ink.duo3.tuned

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import ink.duo3.tuned.worker.InitWorker
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class TunedApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        runBlocking {
            startInitWorker()
        }
    }

    private fun startInitWorker() = WorkManager.getInstance(this).apply {
        enqueueUniqueWork(
            InitWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            InitWorker.startWork()
        )
    }

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}