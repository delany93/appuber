package com.example.uberprofitabilityanalyzer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.uberprofitabilityanalyzer.databinding.ActivitySettingsBinding
import com.example.uberprofitabilityanalyzer.model.UserSettings
import com.example.uberprofitabilityanalyzer.util.AppSettings

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = getString(R.string.settings_activity_title) // Set activity title

        loadSettings()

        binding.buttonSaveSettings.setOnClickListener {
            saveSettings()
        }
    }

    private fun loadSettings() {
        val currentSettings = AppSettings.loadUserSettings(this)
        binding.edittextMinEarnings.setText(currentSettings.minEarningsPerMinute.toString())
        binding.edittextMaxDistance.setText(currentSettings.maxDistanceKm.toString())
    }

    private fun saveSettings() {
        val minEarningsString = binding.edittextMinEarnings.text.toString()
        val maxDistanceString = binding.edittextMaxDistance.text.toString()

        if (minEarningsString.isBlank() || maxDistanceString.isBlank()) {
            Toast.makeText(this, getString(R.string.settings_validation_error_toast), Toast.LENGTH_LONG).show()
            return
        }

        try {
            val minEarnings = minEarningsString.toDouble()
            val maxDistance = maxDistanceString.toDouble()

            if (minEarnings <= 0 || maxDistance <= 0) {
                Toast.makeText(this, getString(R.string.settings_validation_error_toast), Toast.LENGTH_LONG).show()
                return
            }

            val userSettings = UserSettings(
                minEarningsPerMinute = minEarnings,
                maxDistanceKm = maxDistance
            )
            AppSettings.saveUserSettings(this, userSettings)
            Toast.makeText(this, getString(R.string.settings_saved_toast), Toast.LENGTH_SHORT).show()
            // finish() // Optionally close the activity after saving
        } catch (e: NumberFormatException) {
            Toast.makeText(this, getString(R.string.settings_validation_error_toast), Toast.LENGTH_LONG).show()
        }
    }
}
