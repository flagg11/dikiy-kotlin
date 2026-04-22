package com.rut.campusnavigation.domain.model

import org.osmdroid.util.GeoPoint

data class CampusGraph(
    val nodes: List<PathNode>,
    val edges: List<PathEdge>
)

data class PathNode(
    val id: String,
    val location: GeoPoint,
    val type: NodeType = NodeType.WAYPOINT,
    val entranceId: String? = null
)

data class PathEdge(
    val fromId: String,
    val toId: String,
    val distanceM: Double
)

enum class NodeType { WAYPOINT, ENTRANCE, JUNCTION }
