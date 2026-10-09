package com.sputnik.fmsynthesizer.model

import androidx.compose.foundation.text.input.TextFieldState
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
import kotlin.time.Duration.Companion.milliseconds

fun adjustFrequencyForUnpitchedPercussion(freq: Float, envelopeMode: EnvelopeMode): Float {
    val isPercussion = envelopeMode == EnvelopeMode.DRUM || envelopeMode == EnvelopeMode.PERCUSSIVE
    if (!isPercussion || freq <= 1.0f) return freq

    var adjusted = freq
    while (adjusted > 180f) {
        adjusted /= 2f
    }
    return adjusted.coerceAtLeast(60f)
}

fun getFmParametersForInstrumentWithMatchInfo(partName: String, instrumentName: String): FmInstrumentMatchResult {
    val text = "$partName $instrumentName".lowercase()

    return when {
        // Bass Drum / Stortromme / Kick Drum -> c:m = 2:3, I = 1.5, freq = 50 Hz
        text.contains("bass drum") || text.contains("stortromme") || text.contains("kick drum") ||
                text.contains("bdrum") || text.contains("bastromme") || text.contains("bas tromme") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(2, 3), 1.5f, EnvelopeMode.DRUM, overrideFrequencyHz = 50.0f), true)
        }

        // Snare Drum / Lilletromme -> c:m = 2:3, I = 5.0, freq = 150 Hz
        text.contains("snare") || text.contains("lilletromme") || text.contains("lille tromme") ||
                text.contains("snare drum") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(2, 3), 5.0f, EnvelopeMode.DRUM, overrideFrequencyHz = 150.0f), true)
        }

        // Timpani / Pauke -> Pitched deep at ~65 Hz (C-tone)
        text.contains("timpani") || text.contains("pauke") || text.contains("pauker") || text.contains("kettledrum") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(2,3), 1.5f, EnvelopeMode.DRUM, overrideFrequencyHz = 65.41f), true)
        }

        // 1. Maracas / Rumbakugler: c:m = 1:2, I = 12.0, DRUM
        text.contains("maracas") || text.contains("rumbakugler") || text.contains("rumba-kugler") || text.contains("maraca") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(1, 2), 12.0f, EnvelopeMode.DRUM), true)
        }

        // 2. Shaker / Ryster / Cabasa: c:m = 1:2, I = 15.0, DRUM
        text.contains("shaker") || text.contains("ryster") || text.contains("cabasa") || text.contains("shekere") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(1, 2), 15.0f, EnvelopeMode.DRUM), true)
        }

        // 3. Tambourine / Tamburin: c:m = 1:2, I = 14.0, DRUM
        text.contains("tambourine") || text.contains("tamburin") || text.contains("tamborine") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(1, 2), 14.0f, EnvelopeMode.DRUM), true)
        }

        // 4. Claves / Træpinde / Rytmepinde: c:m = 1:2, I = 5.0, PERCUSSIVE
        text.contains("claves") || text.contains("klaves") || text.contains("træpinde") || text.contains("rytmepinde") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(1, 2), 5.0f, EnvelopeMode.PERCUSSIVE), true)
        }

        // 5. Wood block / Træblok / Trætromme: c:m = 16:11 (1.45), I = 5.0, PERCUSSIVE
        text.contains("wood block") || text.contains("woodblock") || text.contains("træblok") ||
                text.contains("wood drum") || text.contains("trætromme") || text.contains("træ tromme") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(2, 3), 1.5f, EnvelopeMode.PERCUSSIVE), true)
        }

        // 6. Cowbell / Koklokke / Ko-klokke: c:m = 16:11 (1.45), I = 8.0, PERCUSSIVE
        text.contains("cowbell") || text.contains("cow bell") || text.contains("koklokke") || text.contains("ko-klokke") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(16, 11), 8.0f, EnvelopeMode.PERCUSSIVE), true)
        }

        // 7. Gong / Gonggong / Tam-tam: c:m = 16:11 (1.45), I = 14.0, FADE
        text.contains("gong") || text.contains("gonggong") || text.contains("tam-tam") || text.contains("tamtam") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(16, 11), 14.0f, EnvelopeMode.FADE), true)
        }

        // 8. Cymbal / Bækken / Hi-hat / Crash / Ride / Splash: c:m = 16:11 (1.45), I = 20.0, DRUM
        text.contains("cymbal") || text.contains("cymbals") || text.contains("bækken") || text.contains("bækkener") ||
                text.contains("crash") || text.contains("ride") || text.contains("hi-hat") || text.contains("hihat") ||
                text.contains("splash") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(16, 11), 20.0f, EnvelopeMode.DRUM), true)
        }

        // 9. Bell / Klokke / Klokkespil / Rørklokke / Kirkeklokke / Glockenspiel / Vibraphone / Celesta / Triangel: c:m = 16:11 (1.45), I = 8.0, PERCUSSIVE
        text.contains("bell") || text.contains("klokke") || text.contains("klokkespil") || text.contains("rørklokke") ||
                text.contains("kirkeklokke") || text.contains("chime") || text.contains("glockenspiel") ||
                text.contains("vibraphone") || text.contains("vibes") || text.contains("celesta") ||
                text.contains("marimba") || text.contains("carillon") || text.contains("campana") ||
                text.contains("triangle") || text.contains("triangel") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(2,3), 8.0f, EnvelopeMode.PERCUSSIVE), true)
        }

        // 10. General Drums / Percussion / Trommer: c:m = 2:3, I = 1.5, freq = 150 Hz
        text.contains("drum") || text.contains("tromme") || text.contains("trætromme") ||
                text.contains("slagtøj") || text.contains("percussion") || text.contains("perkussion") ||
                text.contains("tom") || text.contains("conga") || text.contains("bongo") ||
                text.contains("cajon") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(2, 3), 1.5f, EnvelopeMode.DRUM, overrideFrequencyHz = 150.0f), true)
        }

        // 11. Trumpet / Brass / Messingblæser / Trompet / Kornet: c:m = 1:1, I = 5.0, ADSR
        text.contains("trumpet") || text.contains("trompet") || text.contains("brass") ||
                text.contains("messingblæser") || text.contains("messing") || text.contains("kornet") ||
                text.contains("cornet") || text.contains("bugle") || text.contains("flugelhorn") ||
                text.contains("flygelhorn") || text.contains("tuba") || text.contains("euphonium") ||
                text.contains("barytonhorn") || text.contains("baritone horn") || text.contains("sousaphone") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(1, 1), 5.0f, EnvelopeMode.ADSR), true)
        }

        // 12. Trombone / Basun / Tenorbasun / Basbasun: c:m = 1:1, I = 3.5, ADSR
        text.contains("trombone") || text.contains("basun") || text.contains("tenorbasun") ||
                text.contains("basbasun") || text.contains("posaune") || text.contains("trb") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(1, 1), 3.5f, EnvelopeMode.ADSR), true)
        }

        // 13. French Horn / Horn i F / Valdhorn / Waldhorn: c:m = 1:1, I = 2.5, PAD
        text.contains("french horn") || text.contains("horn i f") || text.contains("horn in f") ||
                text.contains("valdhorn") || text.contains("waldhorn") || text.contains("corno") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(1, 1), 2.5f, EnvelopeMode.PAD), true)
        }

        // 14. Clarinet / Saxophone / Klarinet / Saxofon: c:m = 2:3, I = 3.0, ORGAN
        text.contains("clarinet") || text.contains("klarinet") || text.contains("basset") ||
                text.contains("sax") || text.contains("saxofon") || text.contains("altsax") ||
                text.contains("tenorsax") || text.contains("barytonsax") || text.contains("sopransax") ||
                text.contains("alto sax") || text.contains("tenor sax") || text.contains("baritone sax") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(2, 3), 3.0f, EnvelopeMode.ORGAN), true)
        }

        // 15. Bassoon / Oboe / Fagot / Obo / Engelsk horn: c:m = 1:5, I = 1.0, ADSR
        text.contains("bassoon") || text.contains("fagot") || text.contains("kontrafagot") ||
                text.contains("fagotto") || text.contains("oboe") || text.contains("obo") ||
                text.contains("hautbois") || text.contains("english horn") || text.contains("engelsk horn") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(1, 5), 1.0f, EnvelopeMode.ADSR), true)
        }

        // 16. Flute / Piccolo / Fløjte / Tværfløjte / Pikkolofløjte / Blokfløjte: c:m = 1:1, I = 0.8, AR
        text.contains("flute") || text.contains("fløjte") || text.contains("tværfløjte") ||
                text.contains("pikkolofløjte") || text.contains("blokfløjte") || text.contains("pibe") ||
                text.contains("flote") || text.contains("floete") || text.contains("piccolo") ||
                text.contains("pikkolo") || text.contains("recorder") || text.contains("whistle") ||
                text.contains("fife") || text.contains("pan flute") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(1, 1), 0.8f, EnvelopeMode.AR), true)
        }

        // 17. Organ / Harmonium / Orgel / Kirkeorgel / Pibeorgel / Harmonika: c:m = 1:3, I = 1.0, ORGAN
        text.contains("organ") || text.contains("orgel") || text.contains("kirkeorgel") ||
                text.contains("pibeorgel") || text.contains("trædeorgel") || text.contains("hammond") ||
                text.contains("harmonium") || text.contains("accordion") || text.contains("harmonika") -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(1, 3), 1.0f, EnvelopeMode.ORGAN), true)
        }

        // 18. Unmatched Fallback: Default ratio = 1/1, index = 1.0, ADSR
        else -> {
            FmInstrumentMatchResult(FmInstrumentPreset(Pair(1, 1), 1.0f, EnvelopeMode.ADSR), false)
        }
    }
}
/*
fun getFmParametersForInstrument(partName: String, instrumentName: String): FmInstrumentPreset {
    return getFmParametersForInstrumentWithMatchInfo(partName, instrumentName).preset
}*/

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
    //val selectedInstrumentIndex: StateFlow<Int> = _selectedInstrumentIndex.asStateFlow()

    private val instrumentCm = Array(50) { Pair(1, 1) }
    private val instrumentModIndex = FloatArray(50) { 1.0f }
    private val instrumentEnvelopeMode = Array(50) { EnvelopeMode.ADSR }
    private val instrumentFreqOverride = Array<Float?>(50) { null }

    // Instrument Enable / Solo State
    private val instrumentEnabled = BooleanArray(50) { true }
    private val _soloInstrumentIndex = MutableStateFlow<Int?>(null)
    val soloInstrumentIndex: StateFlow<Int?> = _soloInstrumentIndex.asStateFlow()

    private val _instrumentStateVersion = MutableStateFlow(0)
    //val instrumentStateVersion: StateFlow<Int> = _instrumentStateVersion.asStateFlow()

    // Music Library State
    private val gitHubService = GitHubMusicService

    val githubTokenState = TextFieldState(initialText = DefaultTokenConfig.DEFAULT_TOKEN)

    private val _songListState = MutableStateFlow<SongListState>(SongListState.Loading)
    val songListState: StateFlow<SongListState> = _songListState.asStateFlow()

    private val _selectedSongItem = MutableStateFlow<RemoteSongItem?>(null)
    val selectedSongItem: StateFlow<RemoteSongItem?> = _selectedSongItem.asStateFlow()

    private val _downloadState = MutableStateFlow<SongDownloadState>(SongDownloadState.Idle)
    val downloadState: StateFlow<SongDownloadState> = _downloadState.asStateFlow()

    private val _currentParsedSong = MutableStateFlow<ParsedSong?>(null)
    //val currentParsedSong: StateFlow<ParsedSong?> = _currentParsedSong.asStateFlow()

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

    fun toggleInstrumentEnabled(index: Int) {
        val safeIdx = index.coerceIn(0, 49)
        _soloInstrumentIndex.value = null
        instrumentEnabled[safeIdx] = !instrumentEnabled[safeIdx]
        _instrumentStateVersion.value++
    }

    fun toggleSoloInstrument(index: Int) {
        val safeIdx = index.coerceIn(0, 49)
        if (_soloInstrumentIndex.value == safeIdx) {
            _soloInstrumentIndex.value = null
            for (i in 0..49) instrumentEnabled[i] = true
        } else {
            _soloInstrumentIndex.value = safeIdx
            for (i in 0..49) {
                instrumentEnabled[i] = (i == safeIdx)
            }
        }
        _instrumentStateVersion.value++
    }

    fun isInstrumentEnabled(index: Int): Boolean {
        val safeIdx = index.coerceIn(0, 49)
        return instrumentEnabled[safeIdx]
    }

    fun getInstrumentCm(index: Int): Pair<Int, Int> {
        val safeIdx = index.coerceIn(0, 49)
        return instrumentCm[safeIdx]
    }

    fun getInstrumentModIndex(index: Int): Float {
        val safeIdx = index.coerceIn(0, 49)
        return instrumentModIndex[safeIdx]
    }

    fun getInstrumentEnvelopeMode(index: Int): EnvelopeMode {
        val safeIdx = index.coerceIn(0, 49)
        return instrumentEnvelopeMode[safeIdx]
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
/*
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
    }*/

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
        _cmRatio.value = ratio
        instrumentCm[safeInst] = ratio
        viewModelScope.launch {
            val cm = reduceRatio(ratio)
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
        _cmRatio2.value = ratio
        viewModelScope.launch {
            val cm = reduceRatio(ratio)
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
/*
    // Music Library Functions
    fun setGithubToken(token: String) {
        githubTokenState.setTextAndPlaceCursorAtEnd(token.trim())
        fetchRemoteSongs()
    }*/

    fun fetchRemoteSongs() {
        viewModelScope.launch {
            _songListState.value = SongListState.Loading
            val token = githubTokenState.text.toString().trim()
            val result = gitHubService.fetchSongList(token)
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

    private suspend fun applySongPresets(parsedSong: ParsedSong) {
        _soloInstrumentIndex.value = null
        for (i in 0..49) instrumentEnabled[i] = true

        val numParts = parsedSong.metadata.parts.size.coerceAtLeast(1)
        val polyphonyPerPart = (50 / numParts).coerceIn(1, 4)
        val unmatchedInstruments = mutableListOf<String>()

        parsedSong.metadata.parts.forEachIndexed { partIndex, part ->
            val matchResult = getFmParametersForInstrumentWithMatchInfo(part.name, part.instrumentName)
            val preset = matchResult.preset

            if (!matchResult.isMatched) {
                unmatchedInstruments.add("'${part.name}' (instrument: '${part.instrumentName}')")
            }

            for (pOffset in 0 until polyphonyPerPart) {
                val voiceIdx = (partIndex * polyphonyPerPart + pOffset) % 50
                instrumentCm[voiceIdx] = preset.cmRatio
                instrumentModIndex[voiceIdx] = preset.modulationIndex
                instrumentEnvelopeMode[voiceIdx] = preset.envelopeMode
                instrumentFreqOverride[voiceIdx] = preset.overrideFrequencyHz

                synthesizer.setCMRatio(preset.cmRatio, voiceIdx)
                synthesizer.setModulationIndex(preset.modulationIndex, voiceIdx)
                synthesizer.setEnvelopeMode(preset.envelopeMode.ordinal, voiceIdx)
            }
        }

        if (unmatchedInstruments.isNotEmpty()) {
            println("[FmSynthesizer] Unmatched instruments defaulting to ratio=1/1, index=1.0, ADSR:")
            unmatchedInstruments.forEach { println("   - $it") }
        }

        val currentSelected = _selectedInstrumentIndex.value
        _cmRatio.value = instrumentCm[currentSelected]
        _index.value = instrumentModIndex[currentSelected]
        _envelopeMode.value = instrumentEnvelopeMode[currentSelected]
        _instrumentStateVersion.value++
    }

    fun selectAndDownloadSong(songItem: RemoteSongItem) {
        stopSong()
        _selectedSongItem.value = songItem

        viewModelScope.launch {
            _downloadState.value = SongDownloadState.Downloading
            val token = githubTokenState.text.toString().trim()
            val result = gitHubService.downloadSongInMemory(songItem.downloadUrl, songItem.name, token)
            result.fold(
                onSuccess = { parsedSong ->
                    _currentParsedSong.value = parsedSong
                    _downloadState.value = SongDownloadState.Success(parsedSong)
                    applySongPresets(parsedSong)
                },
                onFailure = { error ->
                    _downloadState.value = SongDownloadState.Error(error.message ?: "Failed to download song")
                }
            )
        }
    }

    fun loadLocalMusicXml(fileName: String, xmlContent: String) {
        loadLocalMusicFile(fileName, xmlContent)
    }

    fun loadLocalMusicFile(fileName: String, content: String) {
        stopSong()
        _selectedSongItem.value = RemoteSongItem(
            name = fileName,
            path = fileName,
            downloadUrl = "local",
            size = content.length.toLong()
        )
        _downloadState.value = SongDownloadState.Downloading

        viewModelScope.launch {
            try {
                val isMidi = fileName.endsWith(".mid", ignoreCase = true) ||
                        fileName.endsWith(".midi", ignoreCase = true) ||
                        content.startsWith("MThd")

                val parsedSong = if (isMidi) {
                    MidiParser().parseSong(content.toLatin1ByteArray(), songNameHint = fileName)
                } else {
                    MusicXmlParser().parseSong(content, songNameHint = fileName)
                }

                _currentParsedSong.value = parsedSong
                _downloadState.value = SongDownloadState.Success(parsedSong)
                applySongPresets(parsedSong)
            } catch (e: Throwable) {
                _downloadState.value = SongDownloadState.Error("Failed to parse local music file: ${e.message}")
            }
        }
    }

    fun playSong(song: ParsedSong) {
        stopSong()
        songPlaybackJob = viewModelScope.launch(Dispatchers.Default) {
            _isPlaying.value = true

            val partIdToPartIndexMap = song.metadata.parts.mapIndexed { index, part ->
                part.id to index
            }.toMap().ifEmpty {
                song.notes.map { it.partId }.distinct().mapIndexed { index, pId ->
                    pId to index
                }.toMap()
            }

            val numParts = song.metadata.parts.size.coerceAtLeast(1)
            val polyphonyPerPart = (50 / numParts).coerceIn(1, 4)
            val partVoiceCounters = mutableMapOf<Int, Int>()

            song.metadata.parts.forEachIndexed { partIndex, part ->
                val matchResult = getFmParametersForInstrumentWithMatchInfo(part.name, part.instrumentName)
                val preset = matchResult.preset

                for (pOffset in 0 until polyphonyPerPart) {
                    val voiceIdx = (partIndex * polyphonyPerPart + pOffset) % 50
                    instrumentCm[voiceIdx] = preset.cmRatio
                    instrumentModIndex[voiceIdx] = preset.modulationIndex
                    instrumentEnvelopeMode[voiceIdx] = preset.envelopeMode
                    instrumentFreqOverride[voiceIdx] = preset.overrideFrequencyHz

                    synthesizer.setCMRatio(preset.cmRatio, voiceIdx)
                    synthesizer.setModulationIndex(preset.modulationIndex, voiceIdx)
                    synthesizer.setEnvelopeMode(preset.envelopeMode.ordinal, voiceIdx)
                }
            }

            val notesByStartTime = song.notes.groupBy { it.startTime }.entries.sortedBy { it.key }
            var currentTimelineMs = 0

            for ((startTimeMs, noteGroup) in notesByStartTime) {

                val delayTime = startTimeMs - currentTimelineMs
                if (delayTime > 0) {
                    delay(delayTime.toLong().milliseconds)
                    currentTimelineMs = startTimeMs
                }

                for (note in noteGroup) {
                    val partIndex = partIdToPartIndexMap[note.partId] ?: 0
                    val pOffset = partVoiceCounters.getOrPut(partIndex) { 0 }
                    partVoiceCounters[partIndex] = (pOffset + 1) % polyphonyPerPart
                    val voiceIndex = (partIndex * polyphonyPerPart + pOffset) % 50

                    if (!instrumentEnabled[voiceIndex]) continue

                    val durationSec = (note.duration.toFloat() / 1000f).coerceAtLeast(0.05f)
                    val envMode = instrumentEnvelopeMode[voiceIndex]
                    val overrideFreq = instrumentFreqOverride[voiceIndex]
                    val playbackFreq = overrideFreq ?: adjustFrequencyForUnpitchedPercussion(
                        note.frequency, envMode)

                    launch {
                        synthesizer.setFrequency(playbackFreq, voiceIndex, durationSec)
                    }
                }
            }

            val totalDurationMs = song.metadata.totalDurationMs
            val remainingMs = totalDurationMs - currentTimelineMs
            if (remainingMs > 0) {
                delay((remainingMs.toLong() + 500L).milliseconds)
            } else {
                delay(500L.milliseconds)
            }

            for (i in 0..49) {
                synthesizer.stop(i)
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
