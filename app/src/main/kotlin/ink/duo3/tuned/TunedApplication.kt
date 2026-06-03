package ink.duo3.tuned

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import ink.duo3.tuned.data.work.FeedRefreshScheduler
import ink.duo3.tuned.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TunedApplication :
    Application(),
    SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        val koinApp =
            startKoin {
                androidLogger()
                androidContext(this@TunedApplication)
                modules(appModules)
            }
        // Keep the daily feed refresh enqueued; KEEP makes this idempotent across launches.
        koinApp.koin.get<FeedRefreshScheduler>().schedule()
    }

    // Coil 3 ships no networking by default; register the OkHttp fetcher so artwork
    // URLs load. One process-wide loader keeps the memory/disk cache shared.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .components { add(OkHttpNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
}
