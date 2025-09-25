package com.example.bugsgame

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    data class GameSettings(
        var gameSpeed: Int = 50,
        var maxCockroaches: Int = 10,
        var bonusInterval: Int = 15,
        var roundDuration: Int = 60
    )

    private val settings = GameSettings()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val sbGameSpeed: SeekBar = view.findViewById(R.id.sbGameSpeed)
        val etMaxCockroaches: EditText = view.findViewById(R.id.etMaxCockroaches)
        val etBonusInterval: EditText = view.findViewById(R.id.etBonusInterval)
        val etRoundDuration: EditText = view.findViewById(R.id.etRoundDuration)
        val btnSaveSettings: Button = view.findViewById(R.id.btnSaveSettings)

        // Set initial values
        sbGameSpeed.progress = settings.gameSpeed
        etMaxCockroaches.setText(settings.maxCockroaches.toString())
        etBonusInterval.setText(settings.bonusInterval.toString())
        etRoundDuration.setText(settings.roundDuration.toString())

        // Save settings
        btnSaveSettings.setOnClickListener {
            settings.gameSpeed = sbGameSpeed.progress
            settings.maxCockroaches = etMaxCockroaches.text.toString().toIntOrNull() ?: settings.maxCockroaches
            settings.bonusInterval = etBonusInterval.text.toString().toIntOrNull() ?: settings.bonusInterval
            settings.roundDuration = etRoundDuration.text.toString().toIntOrNull() ?: settings.roundDuration

            Toast.makeText(context, "Settings saved!", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}