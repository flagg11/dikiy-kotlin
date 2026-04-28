package com.rut.campusnavigation.data.repository

import com.rut.campusnavigation.data.remote.CampusDataSource
import com.rut.campusnavigation.domain.model.*
import com.rut.campusnavigation.domain.repository.CampusRepository
import javax.inject.Inject

class CampusRepositoryImpl @Inject constructor(
    private val dataSource: CampusDataSource
) : CampusRepository {

    override fun getAllBuildings() = dataSource.buildings

    override fun getBuildingById(id: String) = dataSource.buildings.find { it.id == id }

    override fun searchBuildings(query: String): List<Building> {
        if (query.isBlank()) return dataSource.buildings
        val q = query.trim().lowercase()
        return dataSource.buildings.filter {
            it.name.lowercase().contains(q) ||
            it.shortName.lowercase().contains(q) ||
            it.num.toString() == q.removePrefix("корпус").trim()
        }
    }

    override fun searchRooms(query: String): List<Pair<Room, Building>> {
        if (query.isBlank()) return emptyList()
        val q = query.trim()

        // Формат: [КОРПУС][ЭТАЖ][НОМЕР_2ЦИФРЫ]
        // Корпуса 1–9  (1 цифра): мин. 4 знака.  Пример: "8413"  = корпус 8, ауд. 413.
        // Корпуса 10,11,12,14 (2 цифры): мин. 5 знаков. Пример: "10302" = корпус 10, ауд. 302.
        if (q.all { it.isDigit() }) {
            // Сначала пробуем 2-значный корпус (≥5 символов)
            if (q.length >= 5) {
                val bid2 = q.substring(0, 2)
                val b2 = dataSource.buildings.find { it.id == bid2 }
                if (b2 != null) {
                    val room = b2.rooms.find { it.number == q.substring(2) }
                    if (room != null) return listOf(room to b2)
                }
            }
            // Затем 1-значный корпус (≥4 символа)
            if (q.length >= 4) {
                val bid1 = q[0].toString()
                val b1 = dataSource.buildings.find { it.id == bid1 }
                if (b1 != null) {
                    val room = b1.rooms.find { it.number == q.substring(1) }
                    if (room != null) return listOf(room to b1)
                }
            }
        }

        // Текстовый/частичный поиск по всем корпусам
        val qLow = q.lowercase()
        return dataSource.buildings.flatMap { b ->
            b.rooms.filter { r ->
                r.number.contains(q) ||
                r.name.lowercase().contains(qLow) ||
                "${b.num}${r.number}".contains(q)
            }.map { r -> r to b }
        }
    }

    override fun getCampusGraph() = dataSource.graph
}
