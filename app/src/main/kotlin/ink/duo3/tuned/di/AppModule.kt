package ink.duo3.tuned.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Composition root. As layers land, their wiring is added as separate modules
 * (dataModule, playerModule, presentationModule…) and aggregated in [appModules].
 */
val appModule: Module =
    module {
        // No bindings yet — providers are added per build-order step.
    }

val appModules: List<Module> = listOf(appModule, dataModule, playerModule, presentationModule)
