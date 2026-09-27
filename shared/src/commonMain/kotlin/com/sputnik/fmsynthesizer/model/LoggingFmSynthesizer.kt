package com.sputnik.fmsynthesizer.model

import androidx.lifecycle.DefaultLifecycleObserver

expect class LoggingFmSynthesizer(synthesizerHandle: Long = 0) : FmSynthesizer, DefaultLifecycleObserver {
    var synthesizerHandle: Long
    override suspend fun play(instrumentIndex: Int)
    override suspend fun stop(instrumentIndex: Int)
    override suspend fun isPlaying(instrumentIndex: Int): Boolean
    override suspend fun setFrequency(frequencyInHz: Float, instrumentIndex: Int, durationSeconds: Float)
    override suspend fun setModulationIndex(index: Float, instrumentIndex: Int)
    override suspend fun setCMRatio(ratio: Pair<Int, Int>, instrumentIndex: Int)
    override suspend fun setEnvelopeMode(modeIndex: Int, instrumentIndex: Int)

    override suspend fun setModulationIndex2(index: Float, instrumentIndex: Int)
    override suspend fun setCMRatio2(ratio: Pair<Int, Int>, instrumentIndex: Int)

    fun delete()
}
