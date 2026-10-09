package com.sputnik.fmsynthesizer.model

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class LoggingFmSynthesizer actual constructor(
    actual var synthesizerHandle: Long
) : FmSynthesizer, DefaultLifecycleObserver {

    private external fun create(): Long
    private external fun delete(synthesizerHandle: Long)
    private external fun stop(synthesizerHandle: Long, instrumentIndex: Int)
    private external fun play(synthesizerHandle: Long, instrumentIndex: Int)
    private external fun isPlaying(synthesizerHandle: Long, instrumentIndex: Int): Boolean
    private external fun setFrequency(synthesizerHandle: Long, frequencyInHz: Float, instrumentIndex: Int, durationSeconds: Float)
    private external fun setModulationIndex(synthesizerHandle: Long, index: Float, instrumentIndex: Int)
    private external fun setCMRatio(synthesizerHandle: Long, c: Int, m: Int, instrumentIndex: Int)
    private external fun setEnvelopeMode(synthesizerHandle: Long, modeIndex: Int, instrumentIndex: Int)

    private external fun setModulationIndex2(synthesizerHandle: Long, index: Float, instrumentIndex: Int)
    private external fun setCMRatio2(synthesizerHandle: Long, c: Int, m: Int, instrumentIndex: Int)

    private external fun setReverbEnabled(synthesizerHandle: Long, enabled: Boolean)
    private external fun setReverbBalance(synthesizerHandle: Long, balance: Float)
    private external fun setReverbR(synthesizerHandle: Long, r: Float)
    private external fun setReverbG(synthesizerHandle: Long, g: Float)
    private external fun setReverbD(synthesizerHandle: Long, d: Float)

    companion object {
        init {
            loadNativeLibrary("fmsynthesizer_desktop")
        }

        private fun loadNativeLibrary(libName: String) {
            try {
                System.loadLibrary(libName)
            } catch (e: UnsatisfiedLinkError) {
                val os = System.getProperty("os.name").lowercase()
                val ext = when {
                    os.contains("win") -> ".dll"
                    os.contains("mac") -> ".dylib"
                    else -> ".so"
                }
                val prefix = if (os.contains("win")) "" else "lib"
                val resourcePath = "/native/$prefix$libName$ext"
                val stream = LoggingFmSynthesizer::class.java.getResourceAsStream(resourcePath)
                    ?: throw UnsatisfiedLinkError("Native library $resourcePath not found in resources ($resourcePath): ${e.message}")
                val tempFile = File.createTempFile(libName, ext).apply { deleteOnExit() }
                tempFile.outputStream().use { out -> stream.copyTo(out) }
                System.load(tempFile.absolutePath)
            }
        }
    }

    actual override suspend fun play(instrumentIndex: Int) = withContext(Dispatchers.Default) {
        synchronized(lock = synthesizerMutex) {
            createNativeHandleIfNotExists()
            play(synthesizerHandle, instrumentIndex)
        }
    }

    actual override suspend fun stop(instrumentIndex: Int) = withContext(Dispatchers.Default) {
        synchronized(synthesizerMutex) {
            createNativeHandleIfNotExists()
            stop(synthesizerHandle, instrumentIndex)
        }
    }

    actual override suspend fun isPlaying(instrumentIndex: Int): Boolean = withContext(Dispatchers.Default) {
        synchronized(synthesizerMutex) {
            createNativeHandleIfNotExists()
            return@withContext isPlaying(synthesizerHandle, instrumentIndex)
        }
    }

    actual override suspend fun setFrequency(frequencyInHz: Float, instrumentIndex: Int, durationSeconds: Float) = withContext(Dispatchers.Default) {
        synchronized(synthesizerMutex) {
            createNativeHandleIfNotExists()
            setFrequency(synthesizerHandle, frequencyInHz, instrumentIndex, durationSeconds)
        }
    }

    actual override suspend fun setModulationIndex(index: Float, instrumentIndex: Int) = withContext(Dispatchers.Default) {
        synchronized(synthesizerMutex) {
            createNativeHandleIfNotExists()
            setModulationIndex(synthesizerHandle, index, instrumentIndex)
        }
    }

    actual override suspend fun setCMRatio(ratio: Pair<Int, Int>, instrumentIndex: Int) = withContext(Dispatchers.Default) {
        synchronized(synthesizerMutex) {
            createNativeHandleIfNotExists()
            setCMRatio(synthesizerHandle, ratio.first, ratio.second, instrumentIndex)
        }
    }

    actual override suspend fun setEnvelopeMode(modeIndex: Int, instrumentIndex: Int) = withContext(Dispatchers.Default) {
        synchronized(synthesizerMutex) {
            createNativeHandleIfNotExists()
            setEnvelopeMode(synthesizerHandle, modeIndex, instrumentIndex)
        }
    }

    actual override suspend fun setModulationIndex2(index: Float, instrumentIndex: Int) = withContext(Dispatchers.Default) {
        synchronized(synthesizerMutex) {
            createNativeHandleIfNotExists()
            setModulationIndex2(synthesizerHandle, index, instrumentIndex)
        }
    }

    actual override suspend fun setCMRatio2(ratio: Pair<Int, Int>, instrumentIndex: Int) = withContext(Dispatchers.Default) {
        synchronized(synthesizerMutex) {
            createNativeHandleIfNotExists()
            setCMRatio2(synthesizerHandle, ratio.first, ratio.second, instrumentIndex)
        }
    }

    actual override suspend fun setReverbEnabled(enabled: Boolean) = withContext(Dispatchers.Default) {
        synchronized(synthesizerMutex) {
            createNativeHandleIfNotExists()
            setReverbEnabled(synthesizerHandle, enabled)
        }
    }

    actual override suspend fun setReverbBalance(balance: Float) = withContext(Dispatchers.Default) {
        synchronized(synthesizerMutex) {
            createNativeHandleIfNotExists()
            setReverbBalance(synthesizerHandle, balance)
        }
    }

    actual override suspend fun setReverbR(r: Float) = withContext(Dispatchers.Default) {
        synchronized(synthesizerMutex) {
            createNativeHandleIfNotExists()
            setReverbR(synthesizerHandle, r)
        }
    }

    actual override suspend fun setReverbG(g: Float) = withContext(Dispatchers.Default) {
        synchronized(synthesizerMutex) {
            createNativeHandleIfNotExists()
            setReverbG(synthesizerHandle, g)
        }
    }

    actual override suspend fun setReverbD(d: Float) = withContext(Dispatchers.Default) {
        synchronized(synthesizerMutex) {
            createNativeHandleIfNotExists()
            setReverbD(synthesizerHandle, d)
        }
    }

    actual fun delete() {
        synchronized(synthesizerMutex) {
            if (synthesizerHandle != 0L) {
                delete(synthesizerHandle)
                synthesizerHandle = 0L
            }
        }
    }

    private val synthesizerMutex = Any()

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        synchronized(synthesizerMutex) {
            createNativeHandleIfNotExists()
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        delete()
    }

    private fun createNativeHandleIfNotExists() {
        if (synthesizerHandle != 0L) {
            return
        }
        synthesizerHandle = create()
    }
}
