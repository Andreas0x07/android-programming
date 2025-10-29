package com.example.bugsgame.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "players")
data class Player(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val gender: String,
    val course: String,
    val difficulty: Int,
    val birthDate: Long,
    val zodiac: String
)

@Entity(tableName = "scores")
data class Score(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerId: Int,
    val score: Int,
    val difficulty: Int,
    val timestamp: Long
)