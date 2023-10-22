package ink.duo3.tuned.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ink.duo3.tuned.data.database.TunedDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    @Provides
    @Singleton
    fun provideRoomDatabase(
        @ApplicationContext context: Context
    ): TunedDatabase = Room.databaseBuilder(
        context,
        TunedDatabase::class.java,
        "user_database"
    ).build()

    @Provides
    @Singleton
    fun provideSubscriptionDao(
        database: TunedDatabase
    ) = database.subscriptionDao()

    @Provides
    @Singleton
    fun provideRecentlyUpdatedDao(
        database: TunedDatabase
    ) = database.recentlyUpdatedDao()
}