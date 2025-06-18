package com.example.uberprofitabilityanalyzer.model

data class UserSettings(
    val minEarningsPerMinute: Double,
    val maxDistanceKm: Double
    // Future settings could include:
    // - vehicleRunningCostPerKm: Double
    // - preferredPickupRadiusKm: Double
    // - hourlyRateTarget: Double
)
