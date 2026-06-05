package ink.duo3.tuned

import android.app.Application
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
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
    // URLs load. The GIF decoder lets animated chapter art render (some Podcasting 2.0
    // chapters ship GIFs) — ImageDecoder on API 28+, the Movie-based fallback below it.
    // One process-wide loader keeps the memory/disk cache shared.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }.crossfade(true)
            .build()
}
