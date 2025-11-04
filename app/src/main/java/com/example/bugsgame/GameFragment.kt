package com.example.bugsgame

import android.content.Context
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import java.lang.Exception
import kotlin.math.atan2
import kotlin.math.max
import kotlin.random.Random

class GameFragment : Fragment(), SensorEventListener {

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

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var isBonusActive = false
    private var tiltX: Float = 0f
    private var tiltY: Float = 0f

    private data class Velocity(var vx: Float, var vy: Float)
    private val bugVelocities = mutableMapOf<ImageView, Velocity>()

    private lateinit var soundPool: SoundPool
    private var screamSoundId: Int = 0

    private val gameLoop = object : Runnable {
        override fun run() {
            if (!isGameRunning) return

            val bugsToRemove = mutableListOf<ImageView>()
            val gameWidth = gameArea.width
            val gameHeight = gameArea.height

            for (bug in bugVelocities.keys) {
                val velocity = bugVelocities[bug] ?: continue
                val bugWidth = bug.width.toFloat()
                val bugHeight = bug.height.toFloat()

                var newX = bug.x + velocity.vx
                var newY = bug.y + velocity.vy

                if (isBonusActive) {
                    val isPartiallyInside = bug.x + bugWidth > 0 && bug.x < gameWidth &&
                            bug.y + bugHeight > 0 && bug.y < gameHeight

                    if (isPartiallyInside) {
                        newX += tiltX * 2.0f
                        newY += tiltY * 2.0f

                        if (newX < 0f) {
                            newX = 0f
                        } else if (newX > gameWidth - bugWidth) {
                            newX = (gameWidth - bugWidth)
                        }

                        if (newY < 0f) {
                            newY = 0f
                        } else if (newY > gameHeight - bugHeight) {
                            newY = (gameHeight - bugHeight)
                        }

                        bug.x = newX
                        bug.y = newY
                    } else {
                        if (newX < -bugWidth || newX > gameWidth || newY < -bugHeight || newY > gameHeight) {
                            bugsToRemove.add(bug)
                        } else {
                            bug.x = newX
                            bug.y = newY
                        }
                    }
                } else {
                    if (newX < -bugWidth || newX > gameWidth || newY < -bugHeight || newY > gameHeight) {
                        bugsToRemove.add(bug)
                    } else {
                        bug.x = newX
                        bug.y = newY
                    }
                }
            }

            for (bug in bugsToRemove) {
                gameArea.removeView(bug)
                bugVelocities.remove(bug)
                if (bug.tag == "bug") bugCount--
                else if (bug.tag == "bonus") bonusCount--
            }

            handler.postDelayed(this, 16)
        }
    }


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

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val sharedPrefs = requireContext().getSharedPreferences("GameSettings", Context.MODE_PRIVATE)
        settings = SettingsFragment.GameSettings(
            gameSpeed = sharedPrefs.getInt("gameSpeed", 50),
            maxCockroaches = sharedPrefs.getInt("maxCockroaches", 10),
            bonusInterval = sharedPrefs.getInt("bonusInterval", 15),
            roundDuration = sharedPrefs.getInt("roundDuration", 60)
        )

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

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        try {
            screamSoundId = soundPool.load(requireContext(), R.raw.scream, 1)
        } catch (e: Exception) {
            // Sound file not found
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        soundPool.release()
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
        handler.post(gameLoop)

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
        handler.removeCallbacks(gameLoop)

        timerTextView.text = "Time: 0"
        startButton.visibility = View.VISIBLE
        startButton.text = "Play Again"

        gameArea.removeAllViews()
        bugVelocities.clear()
        isBonusActive = false
        tiltX = 0f
        tiltY = 0f

        currentPlayerId?.let { playerId ->
            lifecycleScope.launch {
                val db = DatabaseProvider.getDatabase(requireContext()).appDao()
                val scoreEntry = Score(
                    playerId = playerId,
                    score = score,
                    difficulty = settings.maxCockroaches,
                    timestamp = System.currentTimeMillis(),
                    gameSpeed = settings.gameSpeed,
                    bonusInterval = settings.bonusInterval,
                    roundDuration = settings.roundDuration
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
            bugVelocities.remove(bug)
        }

        val (startX, startY) = getRandomEdgePosition()
        bug.x = startX.toFloat()
        bug.y = startY.toFloat()

        gameArea.addView(bug)
        bugCount++

        val (vx, vy) = getInitialVelocity(bug.x, bug.y)
        bugVelocities[bug] = Velocity(vx, vy)
        val angle = atan2(vy.toDouble(), vx.toDouble()) * (180 / Math.PI)
        bug.rotation = angle.toFloat() + 90f
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
            bugVelocities.remove(bonus)
            activateTiltBonus()
        }

        val (startX, startY) = getRandomEdgePosition()
        bonus.x = startX.toFloat()
        bonus.y = startY.toFloat()

        gameArea.addView(bonus)
        bonusCount++

        val (vx, vy) = getInitialVelocity(bonus.x, bonus.y)
        bugVelocities[bonus] = Velocity(vx, vy)
        val angle = atan2(vy.toDouble(), vx.toDouble()) * (180 / Math.PI)
        bonus.rotation = angle.toFloat() + 90f
    }


    private fun getInitialVelocity(startX: Float, startY: Float): Velocity {
        val endX = if (startX < gameArea.width / 2) gameArea.width.toFloat() + 100f else -100f
        val endY = Random.nextInt(gameArea.height).toFloat()

        val deltaX = endX - startX
        val deltaY = endY - startY
        val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)

        val speed = max(0.1f, settings.gameSpeed / 100f) * 10f

        val vx = (deltaX / distance) * speed
        val vy = (deltaY / distance) * speed

        return Velocity(vx, vy)
    }

    private fun activateTiltBonus() {
        if (isBonusActive) return
        isBonusActive = true
        Toast.makeText(context, "TILT BONUS ACTIVE!", Toast.LENGTH_SHORT).show()

        if (screamSoundId != 0) {
            soundPool.play(screamSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }

        handler.postDelayed({
            isBonusActive = false
            tiltX = 0f
            tiltY = 0f
            Toast.makeText(context, "Tilt bonus ended", Toast.LENGTH_SHORT).show()
        }, 15000)
    }

    private fun getRandomEdgePosition(): Pair<Int, Int> {
        val edge = Random.nextInt(4)
        var x = 0
        var y = 0
        when (edge) {
            0 -> x = -100
            1 -> x = gameArea.width
            2 -> y = -100
            3 -> y = gameArea.height
        }

        if (edge < 2) {
            y = Random.nextInt(gameArea.height)
        } else {
            x = Random.nextInt(gameArea.width)
        }
        return Pair(x, y)
    }

    private fun updateScore() {
        scoreTextView.text = "Score: $score"
    }


    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        val sharedPrefs = requireContext().getSharedPreferences("GameSettings", Context.MODE_PRIVATE)
        currentPlayerId = sharedPrefs.getInt("currentPlayerId", -1).takeIf { it != -1 }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {

            tiltX = -event.values[0]
            tiltY = event.values[1]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}

