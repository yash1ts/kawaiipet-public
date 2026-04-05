package com.kawaiipet.app.di

import android.content.Context
import com.kawaiipet.app.llm.GeminiLlmService
import com.kawaiipet.app.llm.LlmService
import com.kawaiipet.app.memory.db.AppDatabase
import com.kawaiipet.app.memory.db.FactDao
import com.kawaiipet.app.util.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.create(context)

    @Provides
    @Singleton
    fun provideFactDao(database: AppDatabase): FactDao =
        database.factDao()

    @Provides
    @Singleton
    fun providePreferenceManager(@ApplicationContext context: Context): PreferenceManager =
        PreferenceManager(context)

    @Provides
    @Singleton
    fun provideLlmService(preferenceManager: PreferenceManager): LlmService =
        GeminiLlmService(preferenceManager)
}
