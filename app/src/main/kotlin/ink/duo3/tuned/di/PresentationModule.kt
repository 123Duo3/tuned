package ink.duo3.tuned.di

import ink.duo3.tuned.presentation.episode.EpisodeDetailViewModel
import ink.duo3.tuned.presentation.home.HomeViewModel
import ink.duo3.tuned.presentation.library.LibraryViewModel
import ink.duo3.tuned.presentation.player.PlayerViewModel
import ink.duo3.tuned.presentation.podcast.PodcastDetailViewModel
import ink.duo3.tuned.presentation.search.SearchViewModel
import ink.duo3.tuned.presentation.settings.SettingsViewModel
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
        viewModelOf(::PodcastDetailViewModel)
        viewModelOf(::SearchViewModel)
        viewModelOf(::EpisodeDetailViewModel)
        viewModelOf(::PlayerViewModel)
        viewModelOf(::SettingsViewModel)
    }
