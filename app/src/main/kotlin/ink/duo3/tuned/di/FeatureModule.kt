package ink.duo3.tuned.di

import ink.duo3.tuned.feature.library.LibraryViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Wires feature ViewModels. They depend only on domain repository interfaces (bound in
 * [dataModule]), so swapping a data source never touches feature code.
 */
val featureModule: Module =
    module {
        viewModelOf(::LibraryViewModel)
    }
