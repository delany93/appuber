package com.example.uberprofitabilityanalyzer

import com.example.uberprofitabilityanalyzer.model.TripDetails
import com.example.uberprofitabilityanalyzer.model.UserSettings
import android.util.Log

object ProfitabilityCalculator {

    private const val TAG = "ProfitabilityCalculator"

    /**
     * Determines if a given trip is profitable based on user settings.
     *
     * @param tripDetails The details of the trip (price, distance, time).
     * @param userSettings The user-defined settings for profitability (min earnings/min, max distance).
     * @return True if the trip meets the user's profitability criteria, false otherwise.
     */
    fun isTripProfitable(tripDetails: TripDetails, userSettings: UserSettings): Boolean {
        Log.d(TAG, "Calculating profitability for trip: $tripDetails with settings: $userSettings")

        // Handle potential division by zero or invalid time.
        if (tripDetails.timeInMinutes <= 0) {
            Log.w(TAG, "Trip time is zero or negative, considered not profitable.")
            return false
        }

        val earningsPerMinute = tripDetails.price / tripDetails.timeInMinutes
        Log.d(TAG, "Calculated earnings per minute: $earningsPerMinute")

        val meetsMinEarningsCriteria = earningsPerMinute >= userSettings.minEarningsPerMinute
        val meetsMaxDistanceCriteria = tripDetails.distanceInKm <= userSettings.maxDistanceKm

        Log.d(TAG, "Meets min earnings criteria ($earningsPerMinute >= ${userSettings.minEarningsPerMinute}): $meetsMinEarningsCriteria")
        Log.d(TAG, "Meets max distance criteria (${tripDetails.distanceInKm} <= ${userSettings.maxDistanceKm}): $meetsMaxDistanceCriteria")

        return meetsMinEarningsCriteria && meetsMaxDistanceCriteria
    }

    // Future enhancement:
    // data class ProfitabilityResult(
    //     val isProfitable: Boolean,
    //     val calculatedEarningsPerMinute: Double,
    //     val reason: String? = null // e.g., "Below minimum earnings per minute"
    // )
    //
    // fun checkProfitabilityDetailed(tripDetails: TripDetails, userSettings: UserSettings): ProfitabilityResult {
    //     // ... implementation ...
    // }
}
