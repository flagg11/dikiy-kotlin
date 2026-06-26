package com.rut.campusnavigation.domain.model

import org.osmdroid.util.GeoPoint

data class Route(
    val points: List<GeoPoint>,
    val distanceM: Double,
    val indoorSteps: List<String> = emptyList(),
    val fromEntrance: Entrance? = null,
    val toEntrance: Entrance? = null
)
