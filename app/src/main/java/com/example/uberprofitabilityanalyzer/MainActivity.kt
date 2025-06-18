package com.example.uberprofitabilityanalyzer

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.example.uberprofitabilityanalyzer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"
    private val OVERLAY_PERMISSION_REQUEST_CODE = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonToggleOverlayPermission.setOnClickListener {
            checkAndRequestOverlayPermission()
        }

        binding.buttonShowProfitableOverlay.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                val intent = Intent(this, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_SHOW
                    putExtra(OverlayService.EXTRA_IS_PROFITABLE, true)
                    putExtra(OverlayService.EXTRA_DETAILS_TEXT, "PROFITABLE!\n$15.00, 5km, 10min\n$0.75/min")
                }
                startService(intent)
            } else {
                Toast.makeText(this, "Overlay permission not granted.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonShowNotProfitableOverlay.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                val intent = Intent(this, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_SHOW
                    putExtra(OverlayService.EXTRA_IS_PROFITABLE, false)
                    putExtra(OverlayService.EXTRA_DETAILS_TEXT, "NOT PROFITABLE\n$8.00, 10km, 25min\n$0.32/min")
                }
                startService(intent)
            } else {
                Toast.makeText(this, "Overlay permission not granted.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.buttonHideOverlay.setOnClickListener{
             if (Settings.canDrawOverlays(this)) {
                val intent = Intent(this, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_HIDE
                }
                startService(intent)
            } else {
                Toast.makeText(this, "Overlay permission not granted.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonOpenSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // --- TextParserUtil Test UI Logic ---
        binding.buttonTestParse.setOnClickListener {
            val inputText = binding.edittextTestParseInput.text.toString()
            if (inputText.isNotBlank()) {
                val tripDetails = TextParserUtil.parseTextToTripDetails(inputText)
                val resultText = if (tripDetails != null) {
                    "Parsed: Price=${tripDetails.price}, Distance=${tripDetails.distanceInKm}km, Time=${tripDetails.timeInMinutes}min"
                } else {
                    "Failed to parse trip details."
                }
                Log.i(TAG, "TextParserUtil Test: Input='$inputText', Result='$resultText'")
                binding.textviewTestParseResult.text = "Parser Result: $resultText"
                binding.textviewTestParseResult.visibility = View.VISIBLE
                Toast.makeText(this, resultText, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Please enter text to parse.", Toast.LENGTH_SHORT).show()
                binding.textviewTestParseResult.text = "Parser Result: Input was empty."
                binding.textviewTestParseResult.visibility = View.VISIBLE
            }
        }
        // --- End TextParserUtil Test UI Logic ---


        updateButtonVisibility()
    }

    private fun checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.i(TAG, "Requesting overlay permission.")
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        } else {
            Log.i(TAG, "Overlay permission already granted or not required.")
            Toast.makeText(this, "Overlay permission granted or not required.", Toast.LENGTH_SHORT).show();
            // Permission is granted or not needed (below M)
            // Optionally, directly start service or enable test buttons
            updateButtonVisibility(true)
        }
    }

    @Deprecated("This method was deprecated in API level 29.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Log.i(TAG, "Overlay permission GRANTED by user.")
                    Toast.makeText(this, "Overlay permission GRANTED", Toast.LENGTH_SHORT).show()
                    updateButtonVisibility(true)
                } else {
                    Log.w(TAG, "Overlay permission DENIED by user.")
                    Toast.makeText(this, "Overlay permission DENIED", Toast.LENGTH_SHORT).show()
                    updateButtonVisibility(false)
                }
            }
        }
    }

    private fun updateButtonVisibility(hasPermission: Boolean? = null) {
        val canDraw = hasPermission ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
        if (canDraw) {
            binding.buttonShowProfitableOverlay.visibility = View.VISIBLE
            binding.buttonShowNotProfitableOverlay.visibility = View.VISIBLE
            binding.buttonHideOverlay.visibility = View.VISIBLE
        } else {
            binding.buttonShowProfitableOverlay.visibility = View.GONE
            binding.buttonShowNotProfitableOverlay.visibility = View.GONE
            binding.buttonHideOverlay.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        // Update visibility in case permission was changed from settings
        updateButtonVisibility()
    }
}
