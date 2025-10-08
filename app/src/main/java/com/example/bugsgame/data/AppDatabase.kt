package com.example.bugsgame.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Player::class, Score::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        const val DATABASE_NAME = "bugsgame_db"
    }
}