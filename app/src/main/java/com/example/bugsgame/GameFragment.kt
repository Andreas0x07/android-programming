package com.example.bugsgame

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.bugsgame.data.DatabaseProvider
import com.example.bugsgame.data.Score
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.max
import kotlin.random.Random

class GameFragment : Fragment() {

    private lateinit var gameArea: RelativeLayout
    private lateinit var scoreTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var startButton: Button

    private var score = 0
    private var bugCount = 0
    private var bonusCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isGameRunning = false
    private lateinit var settings: SettingsFragment.GameSettings
    private var currentPlayerId: Int? = null

    private val bugSpawner = object : Runnable {
        override fun run() {
            if (isGameRunning && bugCount < settings.maxCockroaches) {
                spawnBug()
                val upperDelay = max(501L, 1500 - settings.bonusInterval * 50L)
                val randomDelay = Random.nextLong(500, upperDelay)
                handler.postDelayed(this, randomDelay)
            }
        }
    }

    private val bonusSpawner = object : Runnable {
        override fun run() {
            if (isGameRunning) {
                spawnBonus()
                val safeInterval = max(1, settings.bonusInterval) * 1000L
                handler.postDelayed(this, safeInterval)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_game, container, false)

        gameArea = view.findViewById(R.id.gameArea)
        scoreTextView = view.findViewById(R.id.tvScore)
        timerTextView = view.findViewById(R.id.tvTimer)
        startButton = view.findViewById(R.id.btnStartGame)

        // Загрузка настроек
        val sharedPrefs = requireContext().getSharedPreferences("GameSettings", Context.MODE_PRIVATE)
        settings = SettingsFragment.GameSettings(
            gameSpeed = sharedPrefs.getInt("gameSpeed", 50),
            maxCockroaches = sharedPrefs.getInt("maxCockroaches", 10),
            bonusInterval = sharedPrefs.getInt("bonusInterval", 15),
            roundDuration = sharedPrefs.getInt("roundDuration", 60)
        )

        // Загрузка ID текущего игрока из SharedPreferences
        currentPlayerId = sharedPrefs.getInt("currentPlayerId", -1).takeIf { it != -1 }

        startButton.setOnClickListener {
            if (currentPlayerId == null) {
                Toast.makeText(context, "Пожалуйста, выберите или зарегистрируйте игрока!", Toast.LENGTH_SHORT).show()
            } else {
                startGame()
            }
        }

        gameArea.setOnClickListener {
            if (isGameRunning) {
                score -= 5
                updateScore()
            }
        }

        return view
    }

    private fun startGame() {
        startButton.visibility = View.GONE
        score = 0
        bugCount = 0
        bonusCount = 0
        updateScore()
        isGameRunning = true
        handler.post(bugSpawner)
        val safeInitialInterval = max(1, settings.bonusInterval) * 1000L
        handler.postDelayed(bonusSpawner, safeInitialInterval)

        val safeDuration = max(1, settings.roundDuration) * 1000L
        object : CountDownTimer(safeDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerTextView.text = "Time: ${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                endGame()
            }
        }.start()
    }

    private fun endGame() {
        isGameRunning = false
        handler.removeCallbacks(bugSpawner)
        handler.removeCallbacks(bonusSpawner)
        timerTextView.text = "Time: 0"
        startButton.visibility = View.VISIBLE
        startButton.text = "Play Again"
        gameArea.removeAllViews()

        // Сохранение очков в базу данных
        currentPlayerId?.let { playerId ->
            lifecycleScope.launch {
                val db = DatabaseProvider.getDatabase(requireContext()).appDao()
                val scoreEntry = Score(
                    playerId = playerId,
                    score = score,
                    difficulty = settings.maxCockroaches, // Используем maxCockroaches как уровень сложности
                    timestamp = System.currentTimeMillis()
                )
                db.insertScore(scoreEntry)
                Toast.makeText(context, "Очки сохранены!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun spawnBug() {
        val bug = ImageView(context)
        bug.setImageResource(R.drawable.bug)
        bug.layoutParams = ViewGroup.LayoutParams(100, 100)
        bug.tag = "bug"

        bug.setOnClickListener {
            score += 10
            bugCount--
            updateScore()
            gameArea.removeView(bug)
        }

        val (startX, startY) = getRandomEdgePosition()
        bug.x = startX.toFloat()
        bug.y = startY.toFloat()

        gameArea.addView(bug)
        bugCount++
        animateBug(bug)
    }

    private fun spawnBonus() {
        if (bonusCount >= 1) return
        val bonus = ImageView(context)
        bonus.setImageResource(R.drawable.bonus_bug)
        bonus.layoutParams = ViewGroup.LayoutParams(100, 100)
        bonus.tag = "bonus"

        bonus.setOnClickListener {
            score += 50
            bonusCount--
            updateScore()
            gameArea.removeView(bonus)
        }

        val (startX, startY) = getRandomEdgePosition()
        bonus.x = startX.toFloat()
        bonus.y = startY.toFloat()

        gameArea.addView(bonus)
        bonusCount++

        animateBug(bonus)
    }

    private fun animateBug(bug: ImageView) {
        val startX = bug.x
        val startY = bug.y

        val endX = if (startX < gameArea.width / 2) gameArea.width.toFloat() else -100f
        val endY = Random.nextInt(gameArea.height).toFloat()

        val deltaX = endX - startX
        val deltaY = endY - startY
        val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
        val speed = max(0.1f, settings.gameSpeed / 100f)
        val duration = (distance / speed).toLong()

        val angle = atan2(deltaY.toDouble(), deltaX.toDouble()) * (180 / Math.PI)
        bug.rotation = angle.toFloat() + 90f

        val animatorX = ObjectAnimator.ofFloat(bug, "translationX", endX)
        val animatorY = ObjectAnimator.ofFloat(bug, "translationY", endY)

        animatorX.duration = duration
        animatorY.duration = duration
        animatorX.interpolator = LinearInterpolator()
        animatorY.interpolator = LinearInterpolator()

        animatorX.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (bug.parent != null) {
                    gameArea.removeView(bug)
                    if (bug.tag == "bug") bugCount--
                    else if (bug.tag == "bonus") bonusCount--
                }
            }
        })

        animatorX.start()
        animatorY.start()
    }

    private fun getRandomEdgePosition(): Pair<Int, Int> {
        val edge = Random.nextInt(4)
        var x = 0
        var y = 0
        when (edge) {
            0 -> x = 0
            1 -> x = gameArea.width - 100
            2 -> y = 0
            3 -> y = gameArea.height - 100
        }
        if (edge < 2) {
            y = Random.nextInt(gameArea.height - 100)
        } else {
            x = Random.nextInt(gameArea.width - 100)
        }
        return Pair(x, y)
    }

    private fun updateScore() {
        scoreTextView.text = "Score: $score"
    }
}