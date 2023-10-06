package com.charyatani.InfoMob.model

data class DeviceInfo(
    val timestamp: String = "",
    val captureCount: Int = 0,
    val frequency: Int = 0,
    val connectivity: Boolean = false,
    val batteryCharging: Boolean = false,
    val batteryCharge: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
