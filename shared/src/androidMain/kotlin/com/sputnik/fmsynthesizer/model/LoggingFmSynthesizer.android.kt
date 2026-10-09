package com.sputnik.fmsynthesizer.model

import androidx.lifecycle.DefaultLifecycleObserver

actual class LoggingFmSynthesizer actual constructor(actual var synthesizerHandle: Long) :
    FmSynthesizer, DefaultLifecycleObserver {
    actual override suspend fun play(instrumentIndex: Int) {
    }

    actual override suspend fun stop(instrumentIndex: Int) {
    }

    actual override suspend fun isPlaying(instrumentIndex: Int): Boolean {
        TODO("Not yet implemented")
    }

    actual override suspend fun setFrequency(frequencyInHz: Float, instrumentIndex: Int, durationSeconds: Float) {
    }

    actual override suspend fun setModulationIndex(index: Float, instrumentIndex: Int) {
    }

    actual override suspend fun setCMRatio(ratio: Pair<Int, Int>, instrumentIndex: Int) {
    }

    actual override suspend fun setEnvelopeMode(modeIndex: Int, instrumentIndex: Int) {
    }

    actual override suspend fun setModulationIndex2(index: Float, instrumentIndex: Int) {
    }

    actual override suspend fun setCMRatio2(ratio: Pair<Int, Int>, instrumentIndex: Int) {
    }

    actual override suspend fun setReverbEnabled(enabled: Boolean) {
    }

    actual override suspend fun setReverbBalance(balance: Float) {
    }

    actual override suspend fun setReverbR(r: Float) {
    }

    actual override suspend fun setReverbG(g: Float) {
    }

    actual override suspend fun setReverbD(d: Float) {
    }

    actual fun delete() {
    }
}
