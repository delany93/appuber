package com.example.uberhelper

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.IBinder
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ScreenCaptureService : Service() {

    private val TAG = "ScreenCaptureService"

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: Implement actual screen capture initiation here.
        // For now, this service is mostly a placeholder for the OCR logic.
        Log.d(TAG, "ScreenCaptureService started")
        // simulateImageProcessing() // Example call
        return START_NOT_STICKY
    }

    fun processImageWithMlKit(bitmap: Bitmap) {
        Log.d(TAG, "Processing image with ML Kit")
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val fullText = visionText.text
                Log.d(TAG, "ML Kit OCR Success: Text recognized")
                // For now, just log the entire text.
                // In a later step, this text will be parsed.
                Log.d(TAG, "Recognized Text: \n$fullText")

                // TODO: Pass this text back to MainActivity or another component for parsing and decision making.
                // This could be done via LocalBroadcastManager, a callback, or by binding to the service.
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "ML Kit OCR Error: ", e)
            }
    }

    // Example function to simulate having a bitmap (remove or modify for actual implementation)
    private fun simulateImageProcessing() {
        // Create a dummy bitmap for testing if needed.
        // In a real scenario, this bitmap would come from MediaProjection.
        try {
            val dummyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            // dummyBitmap.eraseColor(android.graphics.Color.RED) // Example content
            // processImageWithMlKit(dummyBitmap)
            Log.d(TAG, "Simulated image processing would happen here if a bitmap was available.")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating dummy bitmap for simulation", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ScreenCaptureService destroyed")
    }
}
