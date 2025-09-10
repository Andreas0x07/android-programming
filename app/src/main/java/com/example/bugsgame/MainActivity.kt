package com.example.bugsgame

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    // Data structure to hold user info
    data class Player(
        var fullName: String = "",
        var gender: String = "",
        var course: String = "",
        var difficulty: Int = 0,
        var birthDate: Calendar = Calendar.getInstance(),
        var zodiac: String = ""
    )

    private val player = Player()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find views
        val etFullName: EditText = findViewById(R.id.etFullName)
        val rgGender: RadioGroup = findViewById(R.id.rgGender)
        val spCourse: Spinner = findViewById(R.id.spCourse)
        val sbDifficulty: SeekBar = findViewById(R.id.sbDifficulty)
        val cvBirthDate: CalendarView = findViewById(R.id.cvBirthDate)
        val ivZodiac: ImageView = findViewById(R.id.ivZodiac)
        val btnSubmit: Button = findViewById(R.id.btnSubmit)
        val tvOutput: TextView = findViewById(R.id.tvOutput)

        // Populate Spinner with courses (example: 1-5)
        val courses = arrayOf("1st Year", "2nd Year", "3rd Year", "4th Year")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, courses)
        spCourse.adapter = adapter

        // Calendar listener to update birth date
        cvBirthDate.setOnDateChangeListener { _, year, month, day ->
            player.birthDate.set(year, month, day)
            player.zodiac = getZodiacSign(player.birthDate)
            ivZodiac.setImageResource(getZodiacImage(player.zodiac))
        }

        // Submit button click
        btnSubmit.setOnClickListener {
            player.fullName = etFullName.text.toString()
            val selectedGenderId = rgGender.checkedRadioButtonId
            player.gender = if (selectedGenderId == R.id.rbMale) "Male" else "Female"
            player.course = spCourse.selectedItem.toString()
            player.difficulty = sbDifficulty.progress

            // Display data
            tvOutput.text = """
                Full Name: ${player.fullName}
                Gender: ${player.gender}
                Course: ${player.course}
                Difficulty: ${player.difficulty}
                Birth Date: ${player.birthDate.get(Calendar.DAY_OF_MONTH)}/${player.birthDate.get(Calendar.MONTH) + 1}/${player.birthDate.get(Calendar.YEAR)}
                Zodiac: ${player.zodiac}
            """.trimIndent()
        }
    }

    // Function to calculate zodiac sign
    private fun getZodiacSign(date: Calendar): String {
        val day = date.get(Calendar.DAY_OF_MONTH)
        val month = date.get(Calendar.MONTH) + 1  // Months are 0-based
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

    // Function to get zodiac image resource (add your drawable names)
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
            else -> R.drawable.ic_launcher_background  // Fallback to app icon if no match
        }
    }
}