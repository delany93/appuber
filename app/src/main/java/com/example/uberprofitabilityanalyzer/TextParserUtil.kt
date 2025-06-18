package com.example.uberprofitabilityanalyzer

import com.example.uberprofitabilityanalyzer.model.TripDetails
import android.util.Log
import java.util.Locale
import java.util.regex.Pattern

object TextParserUtil {

    private const val TAG = "TextParserUtil"

    // Regex patterns - These are initial guesses and will likely need significant refinement
    // based on actual OCR output from different Uber app versions or locales.
    // Price: Matches common currency symbols ($ R$ € £) and numbers like 1,234.50 or 12.34
    private val PRICE_REGEX = Pattern.compile("""(?:[US\$R$€£]|USD|BRL|EUR|GBP)\s*(\d{1,3}(?:[.,]\d{3})*[.,]\d{2})|(\d+[.,]\d{2})""")
    // Distance: Matches numbers like 5.2 or 5,2 followed by "km" or "kilometers" (case-insensitive)
    // or "mi" or "miles"
    private val DISTANCE_REGEX = Pattern.compile("""(\d+[[.,]\d+)?)\s*(km|kilometers|mi|miles)""", Pattern.CASE_INSENSITIVE)
    // Time: Matches numbers followed by "min" or "minutes" (case-insensitive)
    private val TIME_REGEX = Pattern.compile("""(\d+)\s*(min|minutes)""", Pattern.CASE_INSENSITIVE)


    /**
     * Parses raw text (presumably from OCR or direct node extraction) to extract Uber trip details.
     *
     * This implementation uses initial regex patterns that will likely need refinement
     * based on actual text data from the Uber application.
     *
     * @param text The raw string potentially containing trip details.
     * @return A TripDetails object if parsing is successful for all components, null otherwise.
     */
    fun parseTextToTripDetails(text: String): TripDetails? {
        if (text.isBlank()) {
            Log.w(TAG, "Input text is blank, cannot parse TripDetails.")
            return null
        }
        Log.d(TAG, "Attempting to parse text for trip details: \"$text\"")

        // Normalize text: replace common OCR errors or variations if needed.
        // For example, sometimes 'km' might be read as 'krn'.
        // For now, just converting to lowercase for case-insensitive matching where Pattern flag isn't used.
        val normalizedText = text.replace(",", ".") // Normalize decimal separators to period for parsing
                                 .toLowerCase(Locale.ROOT) // For keywords if not using regex ignore case

        var price: Double? = null
        var distanceInKm: Double? = null
        var timeInMinutes: Double? = null

        // Extract Price
        val priceMatcher = PRICE_REGEX.matcher(normalizedText)
        if (priceMatcher.find()) {
            val priceString = priceMatcher.group(1) ?: priceMatcher.group(2)
            if (priceString != null) {
                try {
                    // Remove any thousands separators if they are periods and decimal is also period
                    // This is tricky; current regex tries to handle common cases.
                    // Assuming the last period is the decimal separator.
                    price = priceString.replace(Regex("""\.(?=\d{3}\.)"""), "") // Remove thousands period if decimal is also period
                                       .toDouble()
                    Log.i(TAG, "Parsed Price: $price")
                } catch (e: NumberFormatException) {
                    Log.e(TAG, "Failed to parse price string: '$priceString'", e)
                }
            }
        } else {
            Log.w(TAG, "Price pattern not found in text: \"$normalizedText\"")
        }

        // Extract Distance
        val distanceMatcher = DISTANCE_REGEX.matcher(normalizedText)
        if (distanceMatcher.find()) {
            val distanceString = distanceMatcher.group(1)
            val unit = distanceMatcher.group(2)?.toLowerCase(Locale.ROOT)
            if (distanceString != null && unit != null) {
                try {
                    var distance = distanceString.toDouble()
                    if (unit == "mi" || unit == "miles") {
                        distance *= 1.60934 // Convert miles to km
                        Log.d(TAG, "Converted $distanceString miles to $distance km")
                    }
                    distanceInKm = distance
                    Log.i(TAG, "Parsed Distance: $distanceInKm km")
                } catch (e: NumberFormatException) {
                    Log.e(TAG, "Failed to parse distance string: '$distanceString'", e)
                }
            }
        } else {
            Log.w(TAG, "Distance pattern not found in text: \"$normalizedText\"")
        }

        // Extract Time
        val timeMatcher = TIME_REGEX.matcher(normalizedText)
        if (timeMatcher.find()) {
            val timeString = timeMatcher.group(1)
            if (timeString != null) {
                try {
                    timeInMinutes = timeString.toDouble()
                    Log.i(TAG, "Parsed Time: $timeInMinutes minutes")
                } catch (e: NumberFormatException) {
                    Log.e(TAG, "Failed to parse time string: '$timeString'", e)
                }
            }
        } else {
            Log.w(TAG, "Time pattern not found in text: \"$normalizedText\"")
        }

        // Return TripDetails only if all components are successfully parsed
        return if (price != null && distanceInKm != null && timeInMinutes != null) {
            TripDetails(price = price, distanceInKm = distanceInKm, timeInMinutes = timeInMinutes)
        } else {
            Log.w(TAG, "Failed to parse one or more trip components: Price=$price, Distance=$distanceInKm, Time=$timeInMinutes")
            null
        }
    }
}
