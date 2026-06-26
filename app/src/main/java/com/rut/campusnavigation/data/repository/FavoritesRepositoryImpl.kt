package com.rut.campusnavigation.data.repository

import com.rut.campusnavigation.data.local.dao.FavoriteDao
import com.rut.campusnavigation.data.local.entity.FavoriteEntity
import com.rut.campusnavigation.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FavoritesRepositoryImpl @Inject constructor(
    private val dao: FavoriteDao
) : FavoritesRepository {
    override fun getAllFavorites(): Flow<List<FavoriteEntity>> = dao.getAllFavorites()
    override fun isFavorite(id: String): Flow<Boolean> = dao.isFavorite(id)
    override suspend fun addFavorite(entity: FavoriteEntity) = dao.addFavorite(entity)
    override suspend fun removeFavorite(id: String) = dao.removeFavorite(id)
}
