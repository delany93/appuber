package com.example.uberhelper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged

class MainActivity : AppCompatActivity() {

    private lateinit var buttonScan: Button
    private lateinit var editTextMinEarnings: EditText
    private lateinit var editTextMaxDistance: EditText
    private lateinit var textViewResult: TextView
    private lateinit var textViewHourlyEarnings: TextView

    private val sharedPrefsName = "UberHelperPreferences"
    private val keyMinEarnings = "minEarnings"
    private val keyMaxDistance = "maxDistance"

    private var isScanning = false
    private lateinit var mediaProjectionManager: MediaProjectionManager
    // private var screenCaptureService: ScreenCaptureService? = null // For bound service later
    // private var isServiceBound = false

    private val screenCaptureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Screen capture permission granted.", Toast.LENGTH_SHORT).show()
            isScanning = true
            buttonScan.text = "Stop Scanning"
            // TODO: Start ScreenCaptureService and pass the result.data (MediaProjection intent)
            // The service will then use MediaProjection to get Bitmaps and call processImageWithMlKit.
            // Example:
            // val serviceIntent = Intent(this, ScreenCaptureService::class.java)
            // serviceIntent.putExtra("RESULT_CODE", result.resultCode)
            // serviceIntent.putExtra("RESULT_DATA", result.data)
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //     startForegroundService(serviceIntent)
            // } else {
            //     startService(serviceIntent)
            // }
            Log.d("MainActivity", "TODO: Start ScreenCaptureService with MediaProjection data.")

        } else {
            Toast.makeText(this, "Screen capture permission denied.", Toast.LENGTH_SHORT).show()
            isScanning = false
            buttonScan.text = "Start Scanning"
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Handle overlay permission result if needed in the future
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission granted.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Overlay permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        buttonScan = findViewById(R.id.buttonScan)
        editTextMinEarnings = findViewById(R.id.editTextMinEarnings)
        editTextMaxDistance = findViewById(R.id.editTextMaxDistance)
        textViewResult = findViewById(R.id.textViewResult)
        textViewHourlyEarnings = findViewById(R.id.textViewHourlyEarnings)

        loadPreferences()

        buttonScan.setOnClickListener {
            if (isScanning) {
                stopScreenCapture()
            } else {
                startScreenCapture()
            }
        }

        editTextMinEarnings.doAfterTextChanged { text ->
            savePreference(keyMinEarnings, text.toString())
        }

        editTextMaxDistance.doAfterTextChanged { text ->
            savePreference(keyMaxDistance, text.toString())
        }
    }

    private fun startScreenCapture() {
        screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private fun stopScreenCapture() {
        isScanning = false
        buttonScan.text = "Start Scanning"
        // TODO: Stop ScreenCaptureService
        // Example:
        // stopService(Intent(this, ScreenCaptureService::class.java))
        Log.d("MainActivity", "TODO: Stop ScreenCaptureService.")
        Toast.makeText(this, "Scanning stopped (placeholder).", Toast.LENGTH_SHORT).show()
    }

    private fun loadPreferences() {
        val sharedPreferences = getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE)
        editTextMinEarnings.setText(sharedPreferences.getString(keyMinEarnings, ""))
        editTextMaxDistance.setText(sharedPreferences.getString(keyMaxDistance, ""))
    }

    private fun savePreference(key: String, value: String) {
        if (value.isNotEmpty()) {
            val sharedPreferences = getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putString(key, value)
                apply()
            }
        }
    }
}
