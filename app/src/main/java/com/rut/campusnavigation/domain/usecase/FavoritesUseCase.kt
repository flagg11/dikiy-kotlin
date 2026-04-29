package com.rut.campusnavigation.domain.usecase

import com.rut.campusnavigation.data.local.entity.FavoriteEntity
import com.rut.campusnavigation.domain.model.Building
import com.rut.campusnavigation.domain.model.Room
import com.rut.campusnavigation.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FavoritesUseCase @Inject constructor(
    private val repository: FavoritesRepository
) {
    fun getAllFavorites(): Flow<List<FavoriteEntity>> = repository.getAllFavorites()
    fun isFavorite(id: String): Flow<Boolean> = repository.isFavorite(id)

    suspend fun toggleBuildingFavorite(building: Building, isFav: Boolean) {
        if (isFav) repository.removeFavorite(building.id)
        else repository.addFavorite(
            FavoriteEntity(building.id, "building", building.name, building.id)
        )
    }

    suspend fun toggleRoomFavorite(room: Room, building: Building, isFav: Boolean) {
        if (isFav) repository.removeFavorite(room.id)
        else repository.addFavorite(
            FavoriteEntity(room.id, "room",
                "Ауд. ${building.num}${room.number}${if (room.name.isNotBlank()) " (${room.name})" else ""}",
                building.id)
        )
    }
}
