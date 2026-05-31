package ink.duo3.tuned.di

import androidx.room.Room
import ink.duo3.tuned.data.local.RoomTransactionRunner
import ink.duo3.tuned.data.local.TransactionRunner
import ink.duo3.tuned.data.local.TunedDatabase
import ink.duo3.tuned.data.network.FeedClient
import ink.duo3.tuned.data.network.RssFeedParser
import ink.duo3.tuned.data.repository.PodcastRepositoryImpl
import ink.duo3.tuned.domain.repository.PodcastRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Wires the data layer: Room database + DAOs, the Ktor/OkHttp client, the RSS
 * parser, and the [PodcastRepository] import pipeline. Feature code depends only on
 * the [PodcastRepository] interface, so the impl stays swappable here.
 */
val dataModule: Module =
    module {
        single { Room.databaseBuilder(androidContext(), TunedDatabase::class.java, "tuned.db").build() }
        single { get<TunedDatabase>().podcastDao() }
        single { get<TunedDatabase>().episodeDao() }
        single { get<TunedDatabase>().progressDao() }
        single<TransactionRunner> { RoomTransactionRunner(get()) }

        single { HttpClient(OkHttp) }
        single { RssFeedParser() }
        single { FeedClient(get()) }

        single<PodcastRepository> {
            PodcastRepositoryImpl(get(), get(), get(), get(), get()) { System.currentTimeMillis() }
        }
    }
