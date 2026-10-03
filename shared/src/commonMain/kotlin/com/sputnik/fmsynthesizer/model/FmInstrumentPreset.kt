package com.sputnik.fmsynthesizer.model


data class FmInstrumentPreset(
    val cmRatio: Pair<Int, Int>,
    val modulationIndex: Float,
    val envelopeMode: EnvelopeMode,
    val overrideFrequencyHz: Float? = null
)
