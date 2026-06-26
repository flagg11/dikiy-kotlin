package com.rut.campusnavigation.domain.model

import org.osmdroid.util.GeoPoint

data class Entrance(
    val id: String,
    val buildingId: String,
    val location: GeoPoint,
    val label: String = "Вход",
    val isMain: Boolean = false
)
