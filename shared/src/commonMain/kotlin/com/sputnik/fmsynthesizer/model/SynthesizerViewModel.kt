package com.sputnik.fmsynthesizer.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class SynthesizerViewModel(
    val synthesizer: FmSynthesizer = LoggingFmSynthesizer()
) : ViewModel() {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _frequency = MutableStateFlow(440f)
    val frequency: StateFlow<Float> = _frequency.asStateFlow()

    // 1st order FM
    private val _index = MutableStateFlow(1f)
    val index: StateFlow<Float> = _index.asStateFlow()

    private val _cmRatio = MutableStateFlow(Pair(1, 1))
    val carrierRatio: StateFlow<Pair<Int, Int>> = _cmRatio.asStateFlow()

    // 2nd order FM
    private val _index2 = MutableStateFlow(0f)
    val index2: StateFlow<Float> = _index2.asStateFlow()

    private val _cmRatio2 = MutableStateFlow(Pair(1, 1))
    val carrierRatio2: StateFlow<Pair<Int, Int>> = _cmRatio2.asStateFlow()

    // Envelope State
    private val _envelopeMode = MutableStateFlow(EnvelopeMode.ADSR)
    val envelopeMode: StateFlow<EnvelopeMode> = _envelopeMode.asStateFlow()

    private val _attack = MutableStateFlow(0.01f)
    val attack: StateFlow<Float> = _attack.asStateFlow()

    private val _decay = MutableStateFlow(0.1f)
    val decay: StateFlow<Float> = _decay.asStateFlow()

    private val _sustain = MutableStateFlow(0.7f)
    val sustain: StateFlow<Float> = _sustain.asStateFlow()

    private val _release = MutableStateFlow(0.3f)
    val release: StateFlow<Float> = _release.asStateFlow()

    // Per-instrument parameters (50 instruments)
    private val _selectedInstrumentIndex = MutableStateFlow(0)
    val selectedInstrumentIndex: StateFlow<Int> = _selectedInstrumentIndex.asStateFlow()

    private val instrumentCm = Array(50) { Pair(1, 2) }
    private val instrumentModIndex = FloatArray(50) { 2.0f }
    private val instrumentEnvelopeMode = Array(50) { EnvelopeMode.ADSR }

    // Music Library State
    private val gitHubService = GitHubMusicService()

    private val _githubToken = MutableStateFlow(DefaultTokenConfig.DEFAULT_TOKEN)
    val githubToken: StateFlow<String> = _githubToken.asStateFlow()

    private val _songListState = MutableStateFlow<SongListState>(SongListState.Loading)
    val songListState: StateFlow<SongListState> = _songListState.asStateFlow()

    private val _selectedSongItem = MutableStateFlow<RemoteSongItem?>(null)
    val selectedSongItem: StateFlow<RemoteSongItem?> = _selectedSongItem.asStateFlow()

    private val _downloadState = MutableStateFlow<SongDownloadState>(SongDownloadState.Idle)
    val downloadState: StateFlow<SongDownloadState> = _downloadState.asStateFlow()

    private val _currentParsedSong = MutableStateFlow<ParsedSong?>(null)
    val currentParsedSong: StateFlow<ParsedSong?> = _currentParsedSong.asStateFlow()

    private var songPlaybackJob: Job? = null

    init {
        viewModelScope.launch {
            synthesizer.setModulationIndex(_index.value)
            synthesizer.setCMRatio(_cmRatio.value)
            synthesizer.setModulationIndex2(_index2.value)
            synthesizer.setCMRatio2(_cmRatio2.value)
        }
        fetchRemoteSongs()
    }

    fun togglePlayStop() {
        viewModelScope.launch {
            if (_isPlaying.value) {
                synthesizer.stop(_selectedInstrumentIndex.value)
                _isPlaying.value = false
            } else {
                synthesizer.setFrequency(_frequency.value, _selectedInstrumentIndex.value)
                synthesizer.play(_selectedInstrumentIndex.value)
                _isPlaying.value = true
            }
        }
    }

    fun selectInstrument(index: Int) {
        val safeIndex = index.coerceIn(0, 49)
        _selectedInstrumentIndex.value = safeIndex
        _cmRatio.value = instrumentCm[safeIndex]
        _index.value = instrumentModIndex[safeIndex]
        _envelopeMode.value = instrumentEnvelopeMode[safeIndex]

        viewModelScope.launch {
            synthesizer.setCMRatio(instrumentCm[safeIndex], safeIndex)
            synthesizer.setModulationIndex(instrumentModIndex[safeIndex], safeIndex)
            synthesizer.setEnvelopeMode(instrumentEnvelopeMode[safeIndex].ordinal, safeIndex)
        }
    }

    fun setFrequency(frequencyInHz: Float) {
        _frequency.value = frequencyInHz
        if (_isPlaying.value) {
            viewModelScope.launch {
                synthesizer.setFrequency(frequencyInHz, _selectedInstrumentIndex.value)
            }
        }
    }

    fun setModulationIndex(indexVal: Float) {
        val safeInst = _selectedInstrumentIndex.value
        _index.value = indexVal
        instrumentModIndex[safeInst] = indexVal
        viewModelScope.launch {
            synthesizer.setModulationIndex(indexVal, safeInst)
        }
    }

    fun setCMRatio(ratio: Pair<Int, Int>) {
        val safeInst = _selectedInstrumentIndex.value
        val cm = reduceRatio(ratio)
        _cmRatio.value = cm
        instrumentCm[safeInst] = cm
        viewModelScope.launch {
            synthesizer.setCMRatio(cm, safeInst)
        }
    }

    fun setModulationIndex2(indexVal: Float) {
        _index2.value = indexVal
        viewModelScope.launch {
            synthesizer.setModulationIndex2(indexVal, _selectedInstrumentIndex.value)
        }
    }

    fun setCMRatio2(ratio: Pair<Int, Int>) {
        val cm = reduceRatio(ratio)
        _cmRatio2.value = cm
        viewModelScope.launch {
            synthesizer.setCMRatio2(cm, _selectedInstrumentIndex.value)
        }
    }

    // Envelope Functions
    fun setEnvelopeMode(mode: EnvelopeMode) {
        val safeInst = _selectedInstrumentIndex.value
        _envelopeMode.value = mode
        instrumentEnvelopeMode[safeInst] = mode
        viewModelScope.launch {
            synthesizer.setEnvelopeMode(mode.ordinal, safeInst)
        }
        when (mode) {
            EnvelopeMode.ADSR -> { _attack.value = 0.01f; _decay.value = 0.1f; _sustain.value = 0.7f; _release.value = 0.3f }
            EnvelopeMode.AD -> { _attack.value = 0.01f; _decay.value = 0.2f; _sustain.value = 0f; _release.value = 0f }
            EnvelopeMode.AR -> { _attack.value = 0.05f; _decay.value = 0f; _sustain.value = 1f; _release.value = 0.5f }
            EnvelopeMode.GATE -> { _attack.value = 0.001f; _decay.value = 0f; _sustain.value = 1f; _release.value = 0.001f }
            EnvelopeMode.PERCUSSIVE -> { _attack.value = 0.001f; _decay.value = 0.08f; _sustain.value = 0.0f; _release.value = 0.05f }
            EnvelopeMode.PLUCK -> { _attack.value = 0.002f; _decay.value = 0.15f; _sustain.value = 0.1f; _release.value = 0.1f }
            EnvelopeMode.PAD -> { _attack.value = 0.5f; _decay.value = 0.3f; _sustain.value = 0.8f; _release.value = 0.8f }
            EnvelopeMode.ORGAN -> { _attack.value = 0.001f; _decay.value = 0f; _sustain.value = 1f; _release.value = 0.02f }
            EnvelopeMode.FADE -> { _attack.value = 0.8f; _decay.value = 0f; _sustain.value = 1f; _release.value = 0.8f }
            EnvelopeMode.DRUM -> { _attack.value = 0.0018f; _decay.value = 0.1f; _sustain.value = 0f; _release.value = 0.01f }
        }
    }

    fun setAttack(value: Float) { _attack.value = value }
    fun setDecay(value: Float) { _decay.value = value }
    fun setSustain(value: Float) { _sustain.value = value }
    fun setRelease(value: Float) { _release.value = value }

    // Music Library Functions
    fun setGithubToken(token: String) {
        _githubToken.value = token.trim()
        fetchRemoteSongs()
    }

    fun fetchRemoteSongs() {
        viewModelScope.launch {
            _songListState.value = SongListState.Loading
            val result = gitHubService.fetchSongList(_githubToken.value)
            result.fold(
                onSuccess = { songs ->
                    _songListState.value = SongListState.Success(songs)
                },
                onFailure = { error ->
                    _songListState.value = SongListState.Error(error.message ?: "Failed to fetch song list")
                }
            )
        }
    }

    fun selectAndDownloadSong(songItem: RemoteSongItem) {
        stopSong()
        _selectedSongItem.value = songItem
        viewModelScope.launch {
            _downloadState.value = SongDownloadState.Downloading
            val result = gitHubService.downloadSongInMemory(songItem.downloadUrl, songItem.name, _githubToken.value)
            result.fold(
                onSuccess = { parsedSong ->
                    _currentParsedSong.value = parsedSong
                    _downloadState.value = SongDownloadState.Success(parsedSong)
                },
                onFailure = { error ->
                    _downloadState.value = SongDownloadState.Error(error.message ?: "Failed to download song")
                }
            )
        }
    }

    fun playSong(song: ParsedSong) {
        stopSong()
        songPlaybackJob = viewModelScope.launch(Dispatchers.Default) {
            _isPlaying.value = true

            val partIdToVoiceMap = song.metadata.parts.mapIndexed { index, part ->
                part.id to index.coerceIn(0, 49)
            }.toMap().ifEmpty {
                song.notes.map { it.partId }.distinct().mapIndexed { index, pId ->
                    pId to index.coerceIn(0, 49)
                }.toMap()
            }

            for ((_, voiceIdx) in partIdToVoiceMap) {
                val cm = instrumentCm[voiceIdx]
                val mod = instrumentModIndex[voiceIdx]
                val envMode = instrumentEnvelopeMode[voiceIdx]
                synthesizer.setCMRatio(cm, voiceIdx)
                synthesizer.setModulationIndex(mod, voiceIdx)
                synthesizer.setEnvelopeMode(envMode.ordinal, voiceIdx)
            }

            val notesByStartTime = song.notes.groupBy { it.startTime }.entries.sortedBy { it.key }
            var currentTimelineMs = 0

            for (entry in notesByStartTime) {
                val startTimeMs = entry.key
                val noteGroup = entry.value

                val delayTime = startTimeMs - currentTimelineMs
                if (delayTime > 0) {
                    delay(delayTime.toLong())
                    currentTimelineMs = startTimeMs
                }

                for (note in noteGroup) {
                    val voiceIndex = partIdToVoiceMap[note.partId] ?: 0
                    val durationSec = (note.duration.toFloat() / 1000f).coerceAtLeast(0.05f)
                    launch {
                        synthesizer.setFrequency(note.frequency, voiceIndex, durationSec)
                    }
                }
            }

            val totalDurationMs = song.metadata.totalDurationMs
            val remainingMs = totalDurationMs - currentTimelineMs
            if (remainingMs > 0) {
                delay(remainingMs.toLong() + 500L)
            } else {
                delay(500L)
            }

            for (voiceIdx in partIdToVoiceMap.values.distinct()) {
                synthesizer.stop(voiceIdx)
            }

            _isPlaying.value = false
        }
    }

    fun stopSong() {
        songPlaybackJob?.cancel()
        songPlaybackJob = null
        _isPlaying.value = false
        viewModelScope.launch {
            for (i in 0..49) {
                synthesizer.stop(i)
            }
        }
    }

    private fun reduceRatio(ratio: Pair<Int, Int>): Pair<Int, Int> {
        val (num, den) = ratio
        if (num == 0 || den == 0) return ratio
        val divisor = gcd(num, den)
        return Pair(num / divisor, den / divisor)
    }

    private tailrec fun gcd(a: Int, b: Int): Int {
        return if (b == 0) abs(a) else gcd(b, a % b)
    }

    override fun onCleared() {
        super.onCleared()
        songPlaybackJob?.cancel()
        (synthesizer as? LoggingFmSynthesizer)?.delete()
    }
}
