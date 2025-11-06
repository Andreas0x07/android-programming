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
import com.example.bugsgame.data.DatabaseProvider
import com.example.bugsgame.data.Score
import kotlinx.coroutines.launch
import java.lang.Exception
import kotlin.math.atan2
import kotlin.math.max
import kotlin.random.Random
import org.koin.androidx.viewmodel.ext.android.viewModel

class GameFragment : Fragment(), SensorEventListener {

    private lateinit var gameArea: RelativeLayout
    private lateinit var scoreTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var startButton: Button

    private val viewModel: GameViewModel by viewModel()
    private val bugViews = mutableMapOf<Int, ImageView>()

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var settings: SettingsFragment.GameSettings
    private var currentPlayerId: Int? = null

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var isTiltBonusActive = false
    private var tiltX: Float = 0f
    private var tiltY: Float = 0f

    private lateinit var soundPool: SoundPool
    private var screamSoundId: Int = 0

    private val BUG_SIZE = 100f
    private val GOLDEN_BUG_SIZE = 120f

    private var gameTimer: CountDownTimer? = null

    private val gameLoop = object : Runnable {
        override fun run() {
            if (!viewModel.isGameRunning) return

            val bugsToRemove = mutableListOf<Int>()
            val gameWidth = gameArea.width
            val gameHeight = gameArea.height

            if (gameWidth == 0 || gameHeight == 0) {
                handler.postDelayed(this, 16)
                return
            }

            for ((id, bugState) in viewModel.bugStates) {
                val bugView = bugViews[id] ?: continue

                val bugSize = if (bugState.type == "golden_bug") GOLDEN_BUG_SIZE else BUG_SIZE
                val bugWidth = bugSize
                val bugHeight = bugSize

                var newX = bugState.x + bugState.vx
                var newY = bugState.y + bugState.vy

                if (isTiltBonusActive) {
                    val isPartiallyInside = bugState.x + bugWidth > 0 && bugState.x < gameWidth &&
                            bugState.y + bugHeight > 0 && bugState.y < gameHeight

                    if (isPartiallyInside) {
                        newX += tiltX * 2.0f
                        newY += tiltY * 2.0f

                        if (newX < 0f) newX = 0f
                        else if (newX > gameWidth - bugWidth) newX = (gameWidth - bugWidth)

                        if (newY < 0f) newY = 0f
                        else if (newY > gameHeight - bugHeight) newY = (gameHeight - bugHeight)

                        bugState.x = newX
                        bugState.y = newY
                        bugView.x = newX
                        bugView.y = newY
                    } else {
                        if (newX < -bugWidth || newX > gameWidth || newY < -bugHeight || newY > gameHeight) {
                            bugsToRemove.add(id)
                        } else {
                            bugState.x = newX
                            bugState.y = newY
                            bugView.x = newX
                            bugView.y = newY
                        }
                    }
                } else {
                    if (newX < -bugWidth || newX > gameWidth || newY < -bugHeight || newY > gameHeight) {
                        bugsToRemove.add(id)
                    } else {
                        bugState.x = newX
                        bugState.y = newY
                        bugView.x = newX
                        bugView.y = newY
                    }
                }
            }

            for (id in bugsToRemove) {
                val bugState = viewModel.bugStates.remove(id)
                gameArea.removeView(bugViews.remove(id))
                when (bugState?.type) {
                    "bug" -> viewModel.bugCount--
                    "bonus" -> viewModel.bonusCount--
                    "golden_bug" -> viewModel.goldenBugCount--
                }
            }

            handler.postDelayed(this, 16)
        }
    }


    private val bugSpawner = object : Runnable {
        override fun run() {
            if (viewModel.isGameRunning) {
                if (viewModel.bugCount < settings.maxCockroaches) {
                    spawnBug()
                }
                val upperDelay = max(501L, 1500 - settings.bonusInterval * 50L)
                val randomDelay = Random.nextLong(500, upperDelay)
                handler.postDelayed(this, randomDelay)
            }
        }
    }

    private val bonusSpawner = object : Runnable {
        override fun run() {
            if (viewModel.isGameRunning) {
                spawnBonus()
                val safeInterval = max(1, settings.bonusInterval) * 1000L
                handler.postDelayed(this, safeInterval)
            }
        }
    }

    private val goldenBugSpawner = object : Runnable {
        override fun run() {
            if (viewModel.isGameRunning) {
                spawnGoldenBug()
                handler.postDelayed(this, 20000L)
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
        currentPlayerId = sharedPrefs.getInt("currentPlayerId", -1).takeIf { it != -1 }


        startButton.setOnClickListener {
            if (currentPlayerId == null) {
                Toast.makeText(context, "Пожалуйста, выберите или зарегистрируйте игрока!", Toast.LENGTH_SHORT).show()
            } else {
                startGame()
            }
        }

        gameArea.setOnClickListener {
            if (viewModel.isGameRunning) {
                viewModel.score -= 5
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
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPrefs = requireContext().getSharedPreferences("GameSettings", Context.MODE_PRIVATE)
        currentPlayerId = sharedPrefs.getInt("currentPlayerId", -1).takeIf { it != -1 }
        updateScore()

        if (viewModel.isGameRunning) {
            restoreGameState()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        gameTimer?.cancel()
        handler.removeCallbacks(gameLoop)
        handler.removeCallbacks(bugSpawner)
        handler.removeCallbacks(bonusSpawner)
        handler.removeCallbacks(goldenBugSpawner)
        soundPool.release()
    }

    private fun startGame() {
        viewModel.resetGame()
        gameArea.removeAllViews()
        bugViews.clear()

        startButton.visibility = View.GONE
        updateScore()
        viewModel.isGameRunning = true

        handler.post(bugSpawner)
        val safeInitialInterval = max(1, settings.bonusInterval) * 1000L
        handler.postDelayed(bonusSpawner, safeInitialInterval)
        handler.postDelayed(goldenBugSpawner, 20000L)
        handler.post(gameLoop)

        val safeDuration = max(1, settings.roundDuration) * 1000L
        viewModel.gameEndTime = System.currentTimeMillis() + safeDuration
        startGameTimer(safeDuration)
    }

    private fun startGameTimer(durationMillis: Long) {
        gameTimer?.cancel()
        gameTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (viewModel.isGameRunning) {
                    timerTextView.text = "Time: ${millisUntilFinished / 1000}"
                }
            }

            override fun onFinish() {
                if (viewModel.isGameRunning) {
                    endGame()
                }
            }
        }.start()
    }

    private fun endGame() {
        viewModel.isGameRunning = false
        gameTimer?.cancel()
        handler.removeCallbacks(bugSpawner)
        handler.removeCallbacks(bonusSpawner)
        handler.removeCallbacks(goldenBugSpawner)
        handler.removeCallbacks(gameLoop)

        timerTextView.text = "Time: 0"
        startButton.visibility = View.VISIBLE
        startButton.text = "Play Again"

        isTiltBonusActive = false
        tiltX = 0f
        tiltY = 0f

        viewModel.saveScore(currentPlayerId ?: -1, settings)
        Toast.makeText(context, "Очки сохранены!", Toast.LENGTH_SHORT).show()

        gameArea.removeAllViews()
        bugViews.clear()
        viewModel.bugStates.clear()
    }

    private fun spawnBug() {
        val (startX, startY) = getRandomEdgePosition()
        val (vx, vy) = getInitialVelocity(startX.toFloat(), startY.toFloat())
        val angle = atan2(vy.toDouble(), vx.toDouble()) * (180 / Math.PI)

        val bugId = viewModel.nextBugId++
        val bugState = GameViewModel.BugState(
            vx = vx,
            vy = vy,
            x = startX.toFloat(),
            y = startY.toFloat(),
            type = "bug",
            rotation = angle.toFloat() + 90f
        )

        viewModel.bugStates[bugId] = bugState
        viewModel.bugCount++
        spawnBugView(bugId, bugState)
    }

    private fun spawnBonus() {
        if (viewModel.bonusCount >= 1) return
        val (startX, startY) = getRandomEdgePosition()
        val (vx, vy) = getInitialVelocity(startX.toFloat(), startY.toFloat())
        val angle = atan2(vy.toDouble(), vx.toDouble()) * (180 / Math.PI)

        val bugId = viewModel.nextBugId++
        val bugState = GameViewModel.BugState(
            vx = vx,
            vy = vy,
            x = startX.toFloat(),
            y = startY.toFloat(),
            type = "bonus",
            rotation = angle.toFloat() + 90f
        )

        viewModel.bugStates[bugId] = bugState
        viewModel.bonusCount++
        spawnBugView(bugId, bugState)
    }

    private fun spawnGoldenBug() {
        if (viewModel.goldenBugCount >= 1) return
        val (startX, startY) = getRandomEdgePosition()
        val (vx, vy) = getInitialVelocity(startX.toFloat(), startY.toFloat())
        val angle = atan2(vy.toDouble(), vx.toDouble()) * (180 / Math.PI)

        val bugId = viewModel.nextBugId++
        val bugState = GameViewModel.BugState(
            vx = vx,
            vy = vy,
            x = startX.toFloat(),
            y = startY.toFloat(),
            type = "golden_bug",
            rotation = angle.toFloat() + 90f
        )

        viewModel.bugStates[bugId] = bugState
        viewModel.goldenBugCount++
        spawnBugView(bugId, bugState)
    }

    private fun spawnBugView(id: Int, bugState: GameViewModel.BugState) {
        val bugView = ImageView(context)
        val (drawable, size, points) = when (bugState.type) {
            "bonus" -> Triple(R.drawable.bonus_bug, BUG_SIZE, 50)
            "golden_bug" -> Triple(R.drawable.bonus_bug, GOLDEN_BUG_SIZE, -1)
            else -> Triple(R.drawable.bug, BUG_SIZE, 10)
        }

        bugView.setImageResource(drawable)
        if (bugState.type == "golden_bug") {
            bugView.setColorFilter(android.graphics.Color.YELLOW)
        }
        bugView.layoutParams = ViewGroup.LayoutParams(size.toInt(), size.toInt())
        bugView.tag = bugState.type

        bugView.setOnClickListener {
            if (!viewModel.isGameRunning) return@setOnClickListener
            val clickPoints = if (points == -1) {
                val sharedPrefs = requireContext().getSharedPreferences("GameSettings", Context.MODE_PRIVATE)
                val goldRate = sharedPrefs.getFloat("gold_rate", 5000f)
                (goldRate / 100).toInt()
            } else {
                points
            }
            onBugClicked(id, bugState.type, clickPoints)
            if (bugState.type == "bonus") {
                activateTiltBonus()
            }
        }

        bugView.x = bugState.x
        bugView.y = bugState.y
        bugView.rotation = bugState.rotation

        gameArea.addView(bugView)
        bugViews[id] = bugView
    }

    private fun onBugClicked(id: Int, type: String, points: Int) {
        if (!viewModel.isGameRunning) return
        viewModel.score += points

        when (type) {
            "bug" -> viewModel.bugCount--
            "bonus" -> viewModel.bonusCount--
            "golden_bug" -> {
                viewModel.goldenBugCount--
                Toast.makeText(context, "+$points (Gold!)", Toast.LENGTH_SHORT).show()
            }
        }

        updateScore()
        gameArea.removeView(bugViews.remove(id))
        viewModel.bugStates.remove(id)
    }


    private fun getInitialVelocity(startX: Float, startY: Float): GameViewModel.BugState {
        val endX = if (startX < gameArea.width / 2) gameArea.width.toFloat() + 100f else -100f
        val endY = Random.nextInt(gameArea.height).toFloat()

        val deltaX = endX - startX
        val deltaY = endY - startY
        val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)

        val speed = max(0.1f, settings.gameSpeed / 100f) * 10f

        val vx = (deltaX / distance) * speed
        val vy = (deltaY / distance) * speed
        val angle = atan2(vy.toDouble(), vx.toDouble()) * (180 / Math.PI)

        return GameViewModel.BugState(vx, vy, startX, startY, "bug", angle.toFloat() + 90f)
    }

    private fun activateTiltBonus() {
        if (viewModel.isBonusActive) return
        viewModel.isBonusActive = true
        isTiltBonusActive = true
        Toast.makeText(context, "TILT BONUS ACTIVE!", Toast.LENGTH_SHORT).show()

        if (screamSoundId != 0) {
            soundPool.play(screamSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }

        val bonusDuration = 15000L
        viewModel.bonusEndTime = System.currentTimeMillis() + bonusDuration
        handler.postDelayed({
            viewModel.isBonusActive = false
            isTiltBonusActive = false
            tiltX = 0f
            tiltY = 0f
            Toast.makeText(context, "Tilt bonus ended", Toast.LENGTH_SHORT).show()
        }, bonusDuration)
    }

    private fun getRandomEdgePosition(): Pair<Int, Int> {
        val edge = Random.nextInt(4)
        var x = 0
        var y = 0
        val width = gameArea.width.takeIf { it > 0 } ?: 1000
        val height = gameArea.height.takeIf { it > 0 } ?: 1000

        when (edge) {
            0 -> x = -100
            1 -> x = width
            2 -> y = -100
            3 -> y = height
        }

        if (edge < 2) {
            y = Random.nextInt(height)
        } else {
            x = Random.nextInt(width)
        }
        return Pair(x, y)
    }

    private fun updateScore() {
        scoreTextView.text = "Score: ${viewModel.score}"
    }

    private fun restoreGameState() {
        startButton.visibility = View.GONE

        for ((id, bugState) in viewModel.bugStates) {
            spawnBugView(id, bugState)
        }

        val remainingTime = viewModel.gameEndTime - System.currentTimeMillis()
        if (remainingTime > 0) {
            startGameTimer(remainingTime)
        } else {
            endGame()
        }

        if (viewModel.isBonusActive) {
            val remainingBonusTime = viewModel.bonusEndTime - System.currentTimeMillis()
            if (remainingBonusTime > 0) {
                isTiltBonusActive = true
                handler.postDelayed({
                    viewModel.isBonusActive = false
                    isTiltBonusActive = false
                    tiltX = 0f
                    tiltY = 0f
                }, remainingBonusTime)
            } else {
                viewModel.isBonusActive = false
                isTiltBonusActive = false
            }
        }

        handler.post(gameLoop)
        handler.post(bugSpawner)
        val safeInitialInterval = max(1, settings.bonusInterval) * 1000L
        handler.postDelayed(bonusSpawner, safeInitialInterval)
        handler.postDelayed(goldenBugSpawner, 20000L)
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