package com.sputnik.fmsynthesizer.model

enum class ReverbPreset(val displayName: String) {
    OFF("Off"),
    ROOM("Room"),
    CONCERT("Concert"),
    HALL("Hall"),
    ECHO_VALLEY("Echo-valley"),
    CUSTOM("Custom")
}

data class ReverbSettings(
    val enabled: Boolean,
    val balance: Float,
    val r: Float,
    val g: Float,
    val d: Float
)
