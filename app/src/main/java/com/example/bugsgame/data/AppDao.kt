package com.example.bugsgame.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Добавление нового игрока
    @Insert
    suspend fun insertPlayer(player: Player)

    // Получение всех игроков
    @Query("SELECT * FROM players")
    fun getAllPlayers(): Flow<List<Player>>

    // Получение игрока по имени
    @Query("SELECT * FROM players WHERE fullName = :fullName LIMIT 1")
    suspend fun getPlayerByName(fullName: String): Player?

    // Добавление рекорда
    @Insert
    suspend fun insertScore(score: Score)

    // Получение всех рекордов с информацией об игроке
    @Query("""
        SELECT scores.*, players.fullName 
        FROM scores 
        INNER JOIN players ON scores.playerId = players.id 
        ORDER BY scores.score DESC
    """)
    fun getAllScores(): Flow<List<ScoreWithPlayer>>

    // Класс для представления рекорда с именем игрока
    data class ScoreWithPlayer(
        val id: Int,
        val playerId: Int,
        val score: Int,
        val difficulty: Int,
        val timestamp: Long,
        val fullName: String
    )
}