package com.gymtracker.app.di

import android.content.Context
import androidx.room.Room
import com.gymtracker.app.data.local.GymTrackerDatabase
import com.gymtracker.app.data.local.dao.GymDao
import com.gymtracker.app.data.repository.GymRepositoryImpl
import com.gymtracker.app.domain.repository.GymRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GymTrackerDatabase =
        Room.databaseBuilder(context, GymTrackerDatabase::class.java, "gymtracker.db")
            .addMigrations(GymTrackerDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideGymDao(database: GymTrackerDatabase): GymDao = database.gymDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindGymRepository(impl: GymRepositoryImpl): GymRepository
}
