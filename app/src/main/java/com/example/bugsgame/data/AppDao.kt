package com.example.bugsgame.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Insert
    suspend fun insertPlayer(player: Player)

    @Query("SELECT * FROM players")
    fun getAllPlayers(): Flow<List<Player>>

    @Query("SELECT * FROM players WHERE fullName = :fullName LIMIT 1")
    suspend fun getPlayerByName(fullName: String): Player?

    @Insert
    suspend fun insertScore(score: Score)

    @Query("""
        SELECT scores.*, players.fullName 
        FROM scores 
        INNER JOIN players ON scores.playerId = players.id 
        ORDER BY scores.score DESC
    """)
    fun getAllScores(): Flow<List<ScoreWithPlayer>>

    data class ScoreWithPlayer(
        val id: Int,
        val playerId: Int,
        val score: Int,
        val difficulty: Int,
        val timestamp: Long,
        val fullName: String,
        val gameSpeed: Int,
        val bonusInterval: Int,
        val roundDuration: Int
    )
}
