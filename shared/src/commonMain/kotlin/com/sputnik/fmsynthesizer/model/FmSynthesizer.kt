package com.sputnik.fmsynthesizer.model

interface FmSynthesizer {
    suspend fun play(instrumentIndex: Int = 0)
    suspend fun stop(instrumentIndex: Int = 0)
    suspend fun isPlaying(instrumentIndex: Int = 0): Boolean
    suspend fun setFrequency(frequencyInHz: Float, instrumentIndex: Int = 0, durationSeconds: Float = 0f)
    suspend fun setModulationIndex(index: Float, instrumentIndex: Int = 0)
    suspend fun setCMRatio(ratio: Pair<Int, Int>, instrumentIndex: Int = 0)
    suspend fun setEnvelopeMode(modeIndex: Int, instrumentIndex: Int = 0)

    // 2nd order FM parameters
    suspend fun setModulationIndex2(index: Float, instrumentIndex: Int = 0)
    suspend fun setCMRatio2(ratio: Pair<Int, Int>, instrumentIndex: Int = 0)
}
