package ink.duo3.tuned.di

import ink.duo3.tuned.presentation.home.HomeViewModel
import ink.duo3.tuned.presentation.library.LibraryViewModel
import ink.duo3.tuned.presentation.search.SearchViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Wires presentation ViewModels. They depend only on domain repository interfaces (bound in
 * [dataModule]), so swapping a data source never touches presentation code.
 */
val presentationModule: Module =
    module {
        viewModelOf(::HomeViewModel)
        viewModelOf(::LibraryViewModel)
        viewModelOf(::SearchViewModel)
    }
