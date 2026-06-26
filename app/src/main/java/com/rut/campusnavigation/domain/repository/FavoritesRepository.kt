package com.rut.campusnavigation.domain.repository

import com.rut.campusnavigation.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun getAllFavorites(): Flow<List<FavoriteEntity>>
    fun isFavorite(id: String): Flow<Boolean>
    suspend fun addFavorite(entity: FavoriteEntity)
    suspend fun removeFavorite(id: String)
}
