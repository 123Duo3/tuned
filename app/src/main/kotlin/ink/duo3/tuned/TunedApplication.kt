package ink.duo3.tuned

import android.app.Application
import ink.duo3.tuned.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TunedApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@TunedApplication)
            modules(appModules)
        }
    }
}
