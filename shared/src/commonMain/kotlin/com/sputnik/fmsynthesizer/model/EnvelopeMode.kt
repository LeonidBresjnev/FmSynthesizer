package com.sputnik.fmsynthesizer.model

enum class EnvelopeMode(val label: String, val description: String) {
    ADSR("ADSR", "Attack, Decay, Sustain, Release"),
    AD("AD", "Attack, Decay"),
    AR("AR", "Attack, Release"),
    GATE("GATE", "Stays constant while note is held"),
    PERCUSSIVE("PERCUSSIVE", "Quick attack followed by decay (Hi-Hat/Cymbals)"),
    PLUCK("PLUCK", "Very fast attack, exponential decay"),
    PAD("PAD", "Slow attack and release"),
    ORGAN("ORGAN", "Immediate attack, constant sustain"),
    FADE("FADE", "Gradual fade in/out"),
    DRUM("DRUM", "1.8ms attack, 100ms decay (Drums, Snare, Tom, Conga)")
}
