package com.rut.campusnavigation.di

import com.rut.campusnavigation.data.repository.CampusRepositoryImpl
import com.rut.campusnavigation.data.repository.FavoritesRepositoryImpl
import com.rut.campusnavigation.domain.repository.CampusRepository
import com.rut.campusnavigation.domain.repository.FavoritesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindCampusRepository(impl: CampusRepositoryImpl): CampusRepository
    @Binds @Singleton
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository
}
