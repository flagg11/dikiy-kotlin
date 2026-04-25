package com.rut.campusnavigation.di

import android.content.Context
import androidx.room.Room
import com.rut.campusnavigation.data.local.CampusDatabase
import com.rut.campusnavigation.data.local.dao.FavoriteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): CampusDatabase =
        Room.databaseBuilder(ctx, CampusDatabase::class.java, "campus_navigation.db").build()

    @Provides
    fun provideFavoriteDao(db: CampusDatabase): FavoriteDao = db.favoriteDao()
}
