package com.rut.campusnavigation.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rut.campusnavigation.data.local.dao.FavoriteDao
import com.rut.campusnavigation.data.local.entity.FavoriteEntity

@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
abstract class CampusDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}
