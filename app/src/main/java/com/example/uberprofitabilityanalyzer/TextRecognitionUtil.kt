package com.example.uberprofitabilityanalyzer

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object TextRecognitionUtil {

    private const val TAG = "TextRecognitionUtil"

    /**
     * Processes a given Bitmap image to extract text using ML Kit Text Recognition.
     *
     * @param bitmap The input image from which to extract text.
     *               // This bitmap would typically be obtained from a screen capture,
     *               // potentially triggered by the UberTripAccessibilityService when a relevant
     *               // notification or UI change is detected.
     * @param onSuccess Callback function invoked when text recognition is successful.
     *                  It receives the extracted text as a single String.
     * @param onFailure Callback function invoked when text recognition fails.
     *                  It receives the exception that occurred.
     */
    fun processImageForText(
        bitmap: Bitmap,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d(TAG, "Starting text recognition process.")
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val resultTextBuilder = StringBuilder()
                for (block in visionText.textBlocks) {
                    // Append text from each block. You could also iterate through lines and elements
                    // if more granularity is needed in the future.
                    resultTextBuilder.append(block.text).append("\n")
                }
                val extractedText = resultTextBuilder.toString().trim()
                Log.i(TAG, "Text recognition successful. Extracted text:\n$extractedText")
                onSuccess(extractedText)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Text recognition failed.", e)
                onFailure(e)
            }
    }
}
