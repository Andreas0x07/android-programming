package com.example.bugsgame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bugsgame.data.AppDao
import com.example.bugsgame.data.Score
import kotlinx.coroutines.launch

class GameViewModel(private val appDao: AppDao) : ViewModel() {

    var score = 0
    var isGameRunning = false
    var gameEndTime = 0L

    var isBonusActive = false
    var bonusEndTime = 0L

    var bugStates = mutableMapOf<Int, BugState>()
    var nextBugId = 0
    var bugCount = 0
    var bonusCount = 0
    var goldenBugCount = 0

    data class BugState(
        var vx: Float,
        var vy: Float,
        var x: Float,
        var y: Float,
        val type: String,
        var rotation: Float
    )

    fun saveScore(playerId: Int, settings: SettingsFragment.GameSettings) {
        if (playerId == -1) return
        viewModelScope.launch {
            val scoreEntry = Score(
                playerId = playerId,
                score = score,
                difficulty = settings.maxCockroaches,
                timestamp = System.currentTimeMillis(),
                gameSpeed = settings.gameSpeed,
                bonusInterval = settings.bonusInterval,
                roundDuration = settings.roundDuration
            )
            appDao.insertScore(scoreEntry)
        }
    }

    fun resetGame() {
        score = 0
        isGameRunning = false
        gameEndTime = 0L
        isBonusActive = false
        bonusEndTime = 0L
        bugStates.clear()
        nextBugId = 0
        bugCount = 0
        bonusCount = 0
        goldenBugCount = 0
    }
}