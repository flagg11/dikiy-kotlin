package com.rut.campusnavigation.domain.repository

import com.rut.campusnavigation.domain.model.Building
import com.rut.campusnavigation.domain.model.CampusGraph
import com.rut.campusnavigation.domain.model.Room

interface CampusRepository {
    fun getAllBuildings(): List<Building>
    fun getBuildingById(id: String): Building?
    fun searchBuildings(query: String): List<Building>
    fun searchRooms(query: String): List<Pair<Room, Building>>
    fun getCampusGraph(): CampusGraph
}
