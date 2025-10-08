package com.example.bugsgame.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

// Сущность для хранения данных игрока
@Entity(tableName = "players")
data class Player(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val gender: String,
    val course: String,
    val difficulty: Int,
    val birthDate: Long, // Храним дату как Long (время в миллисекундах)
    val zodiac: String
)

// Сущность для хранения рекордов
@Entity(tableName = "scores")
data class Score(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerId: Int, // Связь с игроком
    val score: Int,
    val difficulty: Int,
    val timestamp: Long // Время достижения рекорда
)