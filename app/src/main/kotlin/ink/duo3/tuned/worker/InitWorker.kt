package ink.duo3.tuned.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.prof18.rssparser.RssParser
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ink.duo3.tuned.data.dao.RecentlyUpdatedDao
import ink.duo3.tuned.data.dao.SubscriptionDao
import ink.duo3.tuned.data.entity.RecUpdPodEntity
import ink.duo3.tuned.util.pubDateToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class InitWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val rssParser: RssParser,
    private val subscriptionDao: SubscriptionDao,
    private val recentlyUpdatedDao: RecentlyUpdatedDao,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        fetchRecentlyUpdatedPodcast()
        Result.success()
    }

    private suspend fun fetchRecentlyUpdatedPodcast() {
        val subscriptions = subscriptionDao.getSubscriptions()
        val recUpdPodEntityList = mutableListOf<RecUpdPodEntity>()
        subscriptions.forEach { podcast ->
            val rssFeed = runCatching { rssParser.getRssChannel(podcast.url) }

            rssFeed.onSuccess {
                it.items.map { episode ->
                    RecUpdPodEntity(
                        podId = podcast.id!!,
                        enclosure = episode.audio ?: "",
                        podName = podcast.podName,
                        title = episode.title ?: "",
                        description = episode.description ?: "",
                        imageUrl = episode.itunesItemData?.image ?: podcast.imageUrl,
                        pubDateInMs = episode.pubDate?.pubDateToLong() ?: 0L,
                        length = episode.itunesItemData?.duration ?: ""
                    )
                }.let { recUpdPodEntities ->
                    recUpdPodEntityList.addAll(recUpdPodEntities)
                }
            }
        }
        recUpdPodEntityList.sortedByDescending { it.pubDateInMs }.let {
            recentlyUpdatedDao.insertAll(it)
        }
    }

    companion object {
        const val WORK_NAME = "InitWorker"
        fun startWork() = OneTimeWorkRequestBuilder<InitWorker>()
            .addTag(WORK_NAME)
            .build()
    }
}