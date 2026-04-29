package com.rut.campusnavigation.domain.usecase

import com.rut.campusnavigation.domain.model.Building
import com.rut.campusnavigation.domain.model.Room
import com.rut.campusnavigation.domain.repository.CampusRepository
import javax.inject.Inject

sealed class SearchResult {
    data class BuildingResult(val building: Building) : SearchResult()
    data class RoomResult(val room: Room, val building: Building) : SearchResult()
}

class SearchUseCase @Inject constructor(
    private val repo: CampusRepository
) {
    operator fun invoke(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val buildings = repo.searchBuildings(query).map { SearchResult.BuildingResult(it) }
        val rooms = repo.searchRooms(query).map { (r, b) -> SearchResult.RoomResult(r, b) }
        return buildings + rooms
    }
}
