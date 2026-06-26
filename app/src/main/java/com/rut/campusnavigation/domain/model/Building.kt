package com.rut.campusnavigation.domain.model

import org.osmdroid.util.GeoPoint

data class Building(
    val id: String,
    val num: Int,           // Номер корпуса (1-9)
    val name: String,
    val shortName: String,
    val description: String,
    val center: GeoPoint,
    val polygon: List<GeoPoint>,
    val entrances: List<Entrance>,
    val floors: Int,
    val rooms: List<Room> = emptyList(),
    val offCampus: Boolean = false  // вне основного кампуса — маршрут не строится
)
