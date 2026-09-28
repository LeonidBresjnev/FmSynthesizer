package com.sputnik.fmsynthesizer.model

import androidx.lifecycle.DefaultLifecycleObserver
import org.khronos.webgl.Float32Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => new (window.AudioContext || window.webkitAudioContext)()")
private external fun createAudioContextNative(): AudioContext

@OptIn(ExperimentalWasmJsInterop::class)
external class AudioDestinationNode : JsAny

@OptIn(ExperimentalWasmJsInterop::class)
external class AudioContext : JsAny {
    val sampleRate: Double
    val destination: AudioDestinationNode
    fun createScriptProcessor(bufferSize: Int, numberOfInputChannels: Int, numberOfOutputChannels: Int): ScriptProcessorNode
    fun resume()
    fun close()
}

@OptIn(ExperimentalWasmJsInterop::class)
external class ScriptProcessorNode : JsAny {
    var onaudioprocess: (AudioProcessingEvent) -> Unit
    fun connect(destination: JsAny)
    fun disconnect()
}

@OptIn(ExperimentalWasmJsInterop::class)
external class AudioProcessingEvent : JsAny {
    val outputBuffer: AudioBuffer
}

@OptIn(ExperimentalWasmJsInterop::class)
external class AudioBuffer : JsAny {
    fun getChannelData(channel: Int): Float32Array
}

private enum class WebEnvelopeState { Idle, Attack, Decay, Sustain, Release }

private class WebVoice {
    var playnote: Boolean = false
    var targetFrequency: Double = 440.0
    var carrier: Int = 1
    var modulator: Int = 1
    var modulationIndex: Double = 1.0
    var mModDecayScale: Double = 1.0

    var carrier2: Int = 1
    var modulator2: Int = 1
    var modulationIndex2: Double = 0.0

    var fc: Double = 440.0
    var fm: Double = 440.0
    var am: Double = 440.0
    var integral: Double = 0.0
    var tau: Double = 0.0
    var integrand: Double = 0.0

    var envelopeState: WebEnvelopeState = WebEnvelopeState.Idle
    var envelopeValue: Double = 0.0
    var sampleCounter: Double = 0.0
    var noteOffCounter: Double = Double.MAX_VALUE

    var attackStep: Double = 0.001
    var decayStep: Double = 0.001
    var sustainLevel: Double = 0.7
    var releaseStep: Double = 0.001

    fun updateParms() {
        fc = targetFrequency * carrier
        fm = targetFrequency * modulator
        am = modulationIndex * fm
    }

    fun setEnvelopeMode(modeIndex: Int, sampleRate: Double) {
        when (modeIndex) {
            0 -> { attackStep = 1.0 / (0.025 * sampleRate); sustainLevel = 0.4; decayStep = (1.0 - 0.4) / (0.150 * sampleRate); releaseStep = 0.4 / (0.080 * sampleRate) }
            1 -> { attackStep = 1.0 / (0.025 * sampleRate); sustainLevel = 0.0; decayStep = 1.0 / (0.250 * sampleRate); releaseStep = 1.0 / (0.010 * sampleRate) }
            2 -> { attackStep = 1.0 / (0.030 * sampleRate); sustainLevel = 1.0; decayStep = 0.001; releaseStep = 1.0 / (0.300 * sampleRate) }
            3 -> { attackStep = 1.0 / (0.012 * sampleRate); sustainLevel = 1.0; decayStep = 0.001; releaseStep = 1.0 / (0.010 * sampleRate) }
            4 -> { attackStep = 1.0 / (0.002 * sampleRate); sustainLevel = 0.0; decayStep = 1.0 / (0.120 * sampleRate); releaseStep = 1.0 / (0.010 * sampleRate) }
            5 -> { attackStep = 1.0 / (0.001 * sampleRate); sustainLevel = 0.0; decayStep = 1.0 / (0.150 * sampleRate); releaseStep = 1.0 / (0.010 * sampleRate) }
            6 -> { attackStep = 1.0 / (0.400 * sampleRate); sustainLevel = 0.8; decayStep = (1.0 - 0.8) / (0.200 * sampleRate); releaseStep = 0.8 / (0.600 * sampleRate) }
            7 -> { attackStep = 1.0 / (0.008 * sampleRate); sustainLevel = 1.0; decayStep = 0.001; releaseStep = 1.0 / (0.005 * sampleRate) }
            8 -> { attackStep = 1.0 / (0.800 * sampleRate); sustainLevel = 0.7; decayStep = (1.0 - 0.7) / (0.200 * sampleRate); releaseStep = 0.7 / (0.800 * sampleRate) }
            9 -> { attackStep = 1.0 / (0.0018 * sampleRate); sustainLevel = 0.0; decayStep = 1.0 / (0.100 * sampleRate); releaseStep = 1.0 / (0.010 * sampleRate) }
        }
    }
}

actual class LoggingFmSynthesizer actual constructor(
    actual var synthesizerHandle: Long
) : FmSynthesizer, DefaultLifecycleObserver {

    private var audioContext: AudioContext? = null
    private var scriptNode: ScriptProcessorNode? = null

    private var currentSampleRate: Double = 48000.0
    private var sampleTimeStep: Double = 1.0 / 48000.0
    private var modDecayFactorPerSample: Double = 1.0

    private val voices = List(50) { WebVoice() }

    private val tableSize = 512
    private val sineArray = List(tableSize) { i ->
        sin(i * 2.0 * PI / tableSize)
    }
    private val coSineArray = List(tableSize) { i ->
        cos(i * 2.0 * PI / tableSize)
    }

    private fun sine(t: Double): Double {
        val s = t * tableSize
        val i = s.toInt()
        val fraction = s - i
        val idx1 = ((i % tableSize) + tableSize) % tableSize
        val idx2 = ((i + 1) % tableSize + tableSize) % tableSize
        return (1.0 - fraction) * sineArray[idx1] + fraction * sineArray[idx2]
    }

    private fun coSine(t: Double): Float {
        val s = t * tableSize
        val i = s.toInt()
        val fraction = s - i
        val idx1 = ((i % tableSize) + tableSize) % tableSize
        val idx2 = ((i + 1) % tableSize + tableSize) % tableSize
        return ((1.0 - fraction) * coSineArray[idx1] + fraction * coSineArray[idx2]).toFloat()
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private fun initAudio() {
        if (audioContext == null) {
            try {
                val ctx = createAudioContextNative()
                audioContext = ctx
                currentSampleRate = ctx.sampleRate
                sampleTimeStep = 1.0 / currentSampleRate
                modDecayFactorPerSample = 0.5.pow(1.0 / (0.5 * currentSampleRate))

                for (v in voices) {
                    v.setEnvelopeMode(0, currentSampleRate)
                    v.updateParms()
                }

                val node = ctx.createScriptProcessor(1024, 0, 1)
                node.onaudioprocess = { event: AudioProcessingEvent ->
                    val outputData = event.outputBuffer.getChannelData(0)
                    val bufferSize = outputData.length

                    for (i in 0 until bufferSize) {
                        outputData.set(i, 0f)
                    }

                    for (v in voices) {
                        if (!v.playnote) continue

                        for (sample in 0 until bufferSize) {
                            if (v.envelopeState != WebEnvelopeState.Release &&
                                v.envelopeState != WebEnvelopeState.Idle &&
                                v.sampleCounter >= v.noteOffCounter) {
                                v.envelopeState = WebEnvelopeState.Release
                            }

                            when (v.envelopeState) {
                                WebEnvelopeState.Attack -> {
                                    v.envelopeValue += v.attackStep
                                    if (v.envelopeValue >= 1.0) {
                                        v.envelopeValue = 1.0
                                        v.envelopeState = WebEnvelopeState.Decay
                                    }
                                }
                                WebEnvelopeState.Decay -> {
                                    v.envelopeValue -= v.decayStep
                                    if (v.envelopeValue <= v.sustainLevel) {
                                        v.envelopeValue = v.sustainLevel
                                        v.envelopeState = WebEnvelopeState.Sustain
                                    }
                                }
                                WebEnvelopeState.Sustain -> {
                                    v.envelopeValue = v.sustainLevel
                                }
                                WebEnvelopeState.Release -> {
                                    v.envelopeValue -= v.releaseStep
                                    if (v.envelopeValue <= 0.0) {
                                        v.envelopeValue = 0.0
                                        v.envelopeState = WebEnvelopeState.Idle
                                        v.playnote = false
                                    }
                                }
                                WebEnvelopeState.Idle -> {
                                    v.envelopeValue = 0.0
                                    v.playnote = false
                                }
                            }

                            if (v.playnote) {
                                v.sampleCounter += 1.0

                                val effectiveModIdx = v.modulationIndex * v.mModDecayScale
                                v.mModDecayScale *= modDecayFactorPerSample

                                val fcVal = v.targetFrequency * v.carrier
                                val fmVal = v.targetFrequency * v.modulator
                                val amVal = effectiveModIdx * fmVal

                                val index = ((fmVal * (v.tau + sampleTimeStep)) % 1.0 + 1.0) % 1.0
                                val newintegrand = coSine(index).toDouble()
                                v.integral = ((1.0 + v.integral + sampleTimeStep * (fcVal + amVal * (v.integrand + newintegrand) / 2.0)) % 1.0 + 1.0) % 1.0

                                v.integrand = newintegrand
                                v.tau += sampleTimeStep

                                var shapedEnv = v.envelopeValue
                                if (v.envelopeState == WebEnvelopeState.Attack) {
                                    shapedEnv = v.envelopeValue * v.envelopeValue
                                }

                                val sampleVal = (sine(v.integral) * shapedEnv * 0.1).toFloat()
                                val prevVal = outputData.get(sample)
                                outputData.set(sample, prevVal + sampleVal)
                            }
                        }
                    }
                }
                node.connect(ctx.destination)
                scriptNode = node
            } catch (_: Throwable) {
            }
        }
    }

    private fun resumeAudio() {
        try {
            audioContext?.resume()
        } catch (_: Throwable) {}
    }

    actual override suspend fun play(instrumentIndex: Int) {
        initAudio()
        resumeAudio()
        if (instrumentIndex in 0..49) {
            val v = voices[instrumentIndex]
            v.sampleCounter = 0.0
            if (v.envelopeState == WebEnvelopeState.Idle || !v.playnote) {
                v.envelopeValue = 0.0
                v.envelopeState = WebEnvelopeState.Attack
            }
            v.mModDecayScale = 1.0
            v.playnote = true
        }
    }

    actual override suspend fun stop(instrumentIndex: Int) {
        if (instrumentIndex in 0..49) {
            val v = voices[instrumentIndex]
            v.noteOffCounter = v.sampleCounter
            if (v.envelopeState != WebEnvelopeState.Idle) {
                v.envelopeState = WebEnvelopeState.Release
            }
        }
    }

    actual override suspend fun isPlaying(instrumentIndex: Int): Boolean {
        return if (instrumentIndex in 0..49) voices[instrumentIndex].playnote else false
    }

    actual override suspend fun setFrequency(frequencyInHz: Float, instrumentIndex: Int, durationSeconds: Float) {
        if (frequencyInHz > 1.0f) {
            initAudio()
            resumeAudio()
        }
        if (instrumentIndex in 0..49) {
            val v = voices[instrumentIndex]
            if (frequencyInHz > 1.0f) {
                v.targetFrequency = frequencyInHz.toDouble()
                v.sampleCounter = 0.0
                v.mModDecayScale = 1.0

                if (v.envelopeState == WebEnvelopeState.Idle || !v.playnote) {
                    v.envelopeValue = 0.0
                    v.envelopeState = WebEnvelopeState.Attack
                } else if (v.envelopeState == WebEnvelopeState.Release) {
                    v.envelopeState = WebEnvelopeState.Sustain
                }

                if (durationSeconds > 0.0f) {
                    v.noteOffCounter = durationSeconds * currentSampleRate
                } else {
                    v.noteOffCounter = Double.MAX_VALUE
                }
                v.playnote = true
            } else {
                v.noteOffCounter = v.sampleCounter
                if (v.envelopeState != WebEnvelopeState.Idle) {
                    v.envelopeState = WebEnvelopeState.Release
                }
            }
            v.updateParms()
        }
    }

    actual override suspend fun setModulationIndex(index: Float, instrumentIndex: Int) {
        if (instrumentIndex in 0..49) {
            voices[instrumentIndex].modulationIndex = index.toDouble()
            voices[instrumentIndex].updateParms()
        }
    }

    actual override suspend fun setCMRatio(ratio: Pair<Int, Int>, instrumentIndex: Int) {
        if (instrumentIndex in 0..49) {
            voices[instrumentIndex].carrier = ratio.first
            voices[instrumentIndex].modulator = ratio.second
            voices[instrumentIndex].updateParms()
        }
    }

    actual override suspend fun setEnvelopeMode(modeIndex: Int, instrumentIndex: Int) {
        if (instrumentIndex in 0..49) {
            voices[instrumentIndex].setEnvelopeMode(modeIndex, currentSampleRate)
        }
    }

    actual override suspend fun setModulationIndex2(index: Float, instrumentIndex: Int) {
        if (instrumentIndex in 0..49) {
            voices[instrumentIndex].modulationIndex2 = index.toDouble()
        }
    }

    actual override suspend fun setCMRatio2(ratio: Pair<Int, Int>, instrumentIndex: Int) {
        if (instrumentIndex in 0..49) {
            voices[instrumentIndex].carrier2 = ratio.first
            voices[instrumentIndex].modulator2 = ratio.second
        }
    }

    actual fun delete() {
        for (v in voices) v.playnote = false
        try {
            scriptNode?.disconnect()
            audioContext?.close()
        } catch (_: Throwable) {}
        scriptNode = null
        audioContext = null
    }
}
