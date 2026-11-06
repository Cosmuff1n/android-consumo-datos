package com.rojas.consumodatos.di

import android.content.Context
import androidx.room.Room
import com.rojas.consumodatos.data.local.AppDatabase
import com.rojas.consumodatos.data.local.ContactDao
import com.rojas.consumodatos.data.local.ContactLocalDataSource
import com.rojas.consumodatos.data.local.ContactLocalDataSourceImpl
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "contacts_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideContactDao(db: AppDatabase): ContactDao = db.contactDao()

    @Provides
    @Singleton
    fun provideContactLocalDataSource(impl: ContactLocalDataSourceImpl): ContactLocalDataSource = impl
}