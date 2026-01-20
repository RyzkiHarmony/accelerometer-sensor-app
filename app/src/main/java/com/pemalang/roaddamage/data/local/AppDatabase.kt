package com.pemalang.roaddamage.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pemalang.roaddamage.model.Trip

@Database(entities = [Trip::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
}

