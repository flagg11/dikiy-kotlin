package com.rut.campusnavigation.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val type: String,
    val name: String,
    val buildingId: String,
    val addedAt: Long = System.currentTimeMillis()
)
