package ink.duo3.tuned.di

import ink.duo3.tuned.domain.player.PlaybackController
import ink.duo3.tuned.player.media3.Media3PlaybackController
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Wires playback. The Media3 implementation is bound to the [PlaybackController]
 * interface here, so it stays the only media3-aware code the rest of the app sees.
 */
val playerModule: Module =
    module {
        single<PlaybackController> { Media3PlaybackController(androidContext(), get(), get()) }
    }
