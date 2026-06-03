package ink.duo3.tuned.di

import androidx.room.Room
import ink.duo3.tuned.data.local.MIGRATION_1_2
import ink.duo3.tuned.data.local.MIGRATION_2_3
import ink.duo3.tuned.data.local.MIGRATION_3_4
import ink.duo3.tuned.data.local.RoomTransactionRunner
import ink.duo3.tuned.data.local.TransactionRunner
import ink.duo3.tuned.data.local.TunedDatabase
import ink.duo3.tuned.data.network.FeedClient
import ink.duo3.tuned.data.network.FeedResolver
import ink.duo3.tuned.data.network.ItunesSearchApi
import ink.duo3.tuned.data.network.RssFeedParser
import ink.duo3.tuned.data.player.PlaybackResumptionSourceImpl
import ink.duo3.tuned.data.repository.PodcastRepositoryImpl
import ink.duo3.tuned.data.repository.ProgressRepositoryImpl
import ink.duo3.tuned.data.repository.SearchRepositoryImpl
import ink.duo3.tuned.domain.player.PlaybackResumptionSource
import ink.duo3.tuned.domain.repository.PodcastRepository
import ink.duo3.tuned.domain.repository.ProgressRepository
import ink.duo3.tuned.domain.repository.SearchRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json
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
        single {
            Room
                .databaseBuilder(androidContext(), TunedDatabase::class.java, "tuned.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
        }
        single { get<TunedDatabase>().podcastDao() }
        single { get<TunedDatabase>().episodeDao() }
        single { get<TunedDatabase>().progressDao() }
        single<TransactionRunner> { RoomTransactionRunner(get()) }

        single { HttpClient(OkHttp) }
        single { Json { ignoreUnknownKeys = true } }
        single { RssFeedParser() }
        single { FeedClient(get()) }
        single { FeedResolver(get(), get()) }
        single { ItunesSearchApi(get(), get()) }

        single<PodcastRepository> {
            PodcastRepositoryImpl(get(), get(), get(), get()) { System.currentTimeMillis() }
        }
        single<SearchRepository> { SearchRepositoryImpl(get()) }
        single<ProgressRepository> {
            ProgressRepositoryImpl(get()) { System.currentTimeMillis() }
        }
        single<PlaybackResumptionSource> { PlaybackResumptionSourceImpl(get(), get(), get()) }
    }
