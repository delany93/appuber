package com.example.uberprofitabilityanalyzer.util

import android.content.Context
import android.content.SharedPreferences
import com.example.uberprofitabilityanalyzer.model.UserSettings

object AppSettings {

    private const val PREFS_NAME = "uber_profit_analyzer_prefs"
    private const val KEY_MIN_EARNINGS_PER_MINUTE = "min_earnings_per_minute"
    private const val KEY_MAX_DISTANCE_KM = "max_distance_km"

    // Default values
    private const val DEFAULT_MIN_EARNINGS = 0.50 // $0.50 per minute
    private const val DEFAULT_MAX_DISTANCE = 15.0 // 15 km

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveUserSettings(context: Context, settings: UserSettings) {
        val editor = getSharedPreferences(context).edit()
        editor.putFloat(KEY_MIN_EARNINGS_PER_MINUTE, settings.minEarningsPerMinute.toFloat())
        editor.putFloat(KEY_MAX_DISTANCE_KM, settings.maxDistanceKm.toFloat())
        editor.apply()
    }

    fun loadUserSettings(context: Context): UserSettings {
        val prefs = getSharedPreferences(context)
        val minEarnings = prefs.getFloat(KEY_MIN_EARNINGS_PER_MINUTE, DEFAULT_MIN_EARNINGS.toFloat()).toDouble()
        val maxDistance = prefs.getFloat(KEY_MAX_DISTANCE_KM, DEFAULT_MAX_DISTANCE.toFloat()).toDouble()
        return UserSettings(minEarningsPerMinute = minEarnings, maxDistanceKm = maxDistance)
    }

    // Optional: Method to clear settings
    fun clearUserSettings(context: Context) {
        val editor = getSharedPreferences(context).edit()
        editor.remove(KEY_MIN_EARNINGS_PER_MINUTE)
        editor.remove(KEY_MAX_DISTANCE_KM)
        editor.apply()
    }
}
