package com.rut.campusnavigation.domain.model

data class Room(
    val id: String,
    val buildingId: String,
    val number: String,
    val name: String = "",
    val floor: Int,
    val type: RoomType = RoomType.CLASSROOM
)

enum class RoomType {
    CLASSROOM, LABORATORY, OFFICE, AUDITORIUM, CAFETERIA, LIBRARY, GYM, OTHER
}
