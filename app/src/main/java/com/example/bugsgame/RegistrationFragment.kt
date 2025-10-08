package com.example.bugsgame

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.bugsgame.data.DatabaseProvider
import com.example.bugsgame.data.Player
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class RegistrationFragment : Fragment() {

    data class PlayerData(
        var fullName: String = "",
        var gender: String = "",
        var course: String = "",
        var difficulty: Int = 0,
        var birthDate: Calendar = Calendar.getInstance(),
        var zodiac: String = ""
    )

    private val playerData = PlayerData()
    private lateinit var spPlayers: Spinner
    private var selectedPlayer: com.example.bugsgame.data.Player? = null
    private val playersList = mutableListOf<com.example.bugsgame.data.Player>()
    private lateinit var etFullName: EditText
    private lateinit var rgGender: RadioGroup
    private lateinit var spCourse: Spinner
    private lateinit var sbDifficulty: SeekBar
    private lateinit var cvBirthDate: CalendarView
    private lateinit var ivZodiac: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_registration, container, false)

        etFullName = view.findViewById(R.id.etFullName)
        rgGender = view.findViewById(R.id.rgGender)
        spCourse = view.findViewById(R.id.spCourse)
        sbDifficulty = view.findViewById(R.id.sbDifficulty)
        cvBirthDate = view.findViewById(R.id.cvBirthDate)
        ivZodiac = view.findViewById(R.id.ivZodiac)
        val btnSubmit: Button = view.findViewById(R.id.btnSubmit)
        val btnSelectPlayer: Button = view.findViewById(R.id.btnSelectPlayer)
        val tvOutput: TextView = view.findViewById(R.id.tvOutput)
        spPlayers = view.findViewById(R.id.spPlayers)

        // Настройка Spinner для выбора курса
        val courses = arrayOf("1st Year", "2nd Year", "3rd Year", "4th Year")
        val courseAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, courses)
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCourse.adapter = courseAdapter

        // Загрузка списка игроков
        lifecycleScope.launch {
            DatabaseProvider.getDatabase(requireContext()).appDao().getAllPlayers()
                .collectLatest { players ->
                    playersList.clear()
                    playersList.addAll(players)
                    val playerNames = listOf("Выберите игрока") + players.map { it.fullName }
                    val playerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, playerNames)
                    playerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spPlayers.adapter = playerAdapter
                }
        }

        // Выбор игрока через кнопку
        btnSelectPlayer.setOnClickListener {
            val selectedPosition = spPlayers.selectedItemPosition
            if (selectedPosition > 0) { // Пропускаем "Выберите игрока"
                selectedPlayer = playersList[selectedPosition - 1]
                selectedPlayer?.let { player ->
                    etFullName.setText(player.fullName)
                    rgGender.check(if (player.gender == "Male") R.id.rbMale else R.id.rbFemale)
                    spCourse.setSelection(courses.indexOf(player.course))
                    sbDifficulty.progress = player.difficulty
                    cvBirthDate.date = player.birthDate
                    playerData.zodiac = player.zodiac
                    ivZodiac.setImageResource(getZodiacImage(player.zodiac))
                    Toast.makeText(context, "Игрок ${player.fullName} выбран", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Пожалуйста, выберите игрока из списка", Toast.LENGTH_SHORT).show()
            }
        }

        // Изменение даты рождения
        cvBirthDate.setOnDateChangeListener { _, year, month, day ->
            playerData.birthDate.set(year, month, day)
            playerData.zodiac = getZodiacSign(playerData.birthDate)
            ivZodiac.setImageResource(getZodiacImage(playerData.zodiac))
        }

        // Сохранение игрока
        btnSubmit.setOnClickListener {
            playerData.fullName = etFullName.text.toString()
            if (playerData.fullName.isBlank()) {
                Toast.makeText(context, "Введите имя игрока", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedGenderId = rgGender.checkedRadioButtonId
            if (selectedGenderId == -1) {
                Toast.makeText(context, "Выберите пол", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            playerData.gender = if (selectedGenderId == R.id.rbMale) "Male" else "Female"
            playerData.course = spCourse.selectedItem?.toString() ?: ""
            playerData.difficulty = sbDifficulty.progress

            lifecycleScope.launch {
                val db = DatabaseProvider.getDatabase(requireContext()).appDao()
                val existingPlayer = db.getPlayerByName(playerData.fullName)
                val playerId: Int
                if (existingPlayer == null) {
                    val newPlayer = com.example.bugsgame.data.Player(
                        fullName = playerData.fullName,
                        gender = playerData.gender,
                        course = playerData.course,
                        difficulty = playerData.difficulty,
                        birthDate = playerData.birthDate.timeInMillis,
                        zodiac = playerData.zodiac
                    )
                    db.insertPlayer(newPlayer)
                    playerId = db.getPlayerByName(playerData.fullName)?.id ?: 0
                    Toast.makeText(context, "Игрок сохранён!", Toast.LENGTH_SHORT).show()
                } else {
                    playerId = existingPlayer.id
                    Toast.makeText(context, "Игрок с таким именем уже существует!", Toast.LENGTH_SHORT).show()
                }

                // Сохранение ID текущего игрока
                val sharedPrefs = requireContext().getSharedPreferences("GameSettings", Context.MODE_PRIVATE)
                sharedPrefs.edit().putInt("currentPlayerId", playerId).apply()

                tvOutput.text = """
                    Full Name: ${playerData.fullName}
                    Gender: ${playerData.gender}
                    Course: ${playerData.course}
                    Difficulty: ${playerData.difficulty}
                    Birth Date: ${playerData.birthDate.get(Calendar.DAY_OF_MONTH)}/${playerData.birthDate.get(Calendar.MONTH) + 1}/${playerData.birthDate.get(Calendar.YEAR)}
                    Zodiac: ${playerData.zodiac}
                """.trimIndent()
            }
        }

        // Очистка полей по умолчанию
        clearForm()

        return view
    }

    private fun clearForm() {
        etFullName.setText("")
        rgGender.clearCheck()
        spCourse.setSelection(0)
        sbDifficulty.progress = 0
        playerData.birthDate = Calendar.getInstance()
        cvBirthDate.date = playerData.birthDate.timeInMillis
        playerData.zodiac = getZodiacSign(playerData.birthDate)
        ivZodiac.setImageResource(getZodiacImage(playerData.zodiac))
        selectedPlayer = null
    }

    private fun getZodiacSign(date: Calendar): String {
        val day = date.get(Calendar.DAY_OF_MONTH)
        val month = date.get(Calendar.MONTH) + 1
        return when {
            (month == 3 && day >= 21) || (month == 4 && day <= 19) -> "Aries"
            (month == 4 && day >= 20) || (month == 5 && day <= 20) -> "Taurus"
            (month == 5 && day >= 21) || (month == 6 && day <= 20) -> "Gemini"
            (month == 6 && day >= 21) || (month == 7 && day <= 22) -> "Cancer"
            (month == 7 && day >= 23) || (month == 8 && day <= 22) -> "Leo"
            (month == 8 && day >= 23) || (month == 9 && day <= 22) -> "Virgo"
            (month == 9 && day >= 23) || (month == 10 && day <= 22) -> "Libra"
            (month == 10 && day >= 23) || (month == 11 && day <= 21) -> "Scorpio"
            (month == 11 && day >= 22) || (month == 12 && day <= 21) -> "Sagittarius"
            (month == 12 && day >= 22) || (month == 1 && day <= 19) -> "Capricorn"
            (month == 1 && day >= 20) || (month == 2 && day <= 18) -> "Aquarius"
            else -> "Pisces"
        }
    }

    private fun getZodiacImage(zodiac: String): Int {
        return when (zodiac) {
            "Aries" -> R.drawable.aries
            "Taurus" -> R.drawable.taurus
            "Gemini" -> R.drawable.gemini
            "Cancer" -> R.drawable.cancer
            "Leo" -> R.drawable.leo
            "Virgo" -> R.drawable.virgo
            "Libra" -> R.drawable.libra
            "Scorpio" -> R.drawable.scorpio
            "Sagittarius" -> R.drawable.sagittarius
            "Capricorn" -> R.drawable.capricorn
            "Aquarius" -> R.drawable.aquarius
            "Pisces" -> R.drawable.pisces
            else -> R.drawable.ic_launcher_background
        }
    }
}

