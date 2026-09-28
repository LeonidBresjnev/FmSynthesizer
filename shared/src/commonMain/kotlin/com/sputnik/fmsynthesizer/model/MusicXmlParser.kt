package com.sputnik.fmsynthesizer.model

import kotlin.math.abs
import kotlin.math.pow

data class ParsedMusicNote(
    val startTime: Int,
    val duration: Int,
    val frequency: Float,
    val partId: String,
    val voice: Int,
    val isSlur: Boolean = false,
    val isTieStart: Boolean = false,
    val isTieStop: Boolean = false
)

data class MusicPartInfo(
    val id: String,
    val name: String,
    val instrumentName: String = "",
    val noteCount: Int = 0,
    val initialVolumeDb: Float? = null
)

data class SongMetadata(
    val title: String = "",
    val composer: String = "",
    val parts: List<MusicPartInfo> = emptyList(),
    val totalNotes: Int = 0,
    val totalDurationMs: Int = 0,
    val isSwing: Boolean = false
)

data class ParsedSong(
    val metadata: SongMetadata,
    val notes: List<ParsedMusicNote>
)

data class RemoteSongItem(
    val name: String,
    val path: String,
    val downloadUrl: String,
    val size: Long
)

sealed class SongListState {
    object Loading : SongListState()
    data class Success(val songs: List<RemoteSongItem>) : SongListState()
    data class Error(val message: String) : SongListState()
}

sealed class SongDownloadState {
    object Idle : SongDownloadState()
    object Downloading : SongDownloadState()
    data class Success(val song: ParsedSong) : SongDownloadState()
    data class Error(val message: String) : SongDownloadState()
}

class XmlNode(
    val name: String,
    val attributes: Map<String, String> = emptyMap(),
    val children: List<XmlNode> = emptyList(),
    val textContent: String = ""
) {
    fun getElementsByTagName(tagName: String): List<XmlNode> {
        val result = mutableListOf<XmlNode>()
        fun collect(node: XmlNode) {
            if (node.name.equals(tagName, ignoreCase = true)) {
                result.add(node)
            }
            for (child in node.children) {
                collect(child)
            }
        }
        for (child in children) {
            collect(child)
        }
        return result
    }

    fun getAttribute(attrName: String): String {
        return attributes[attrName] ?: attributes[attrName.lowercase()] ?: ""
    }
}

class MusicXmlParser {

    internal data class ParsedNoteEvent(
        val relativeStartDivisions: Int = 0,
        val durationDivisions: Int = 1,
        val noteData: TempNoteData? = null,
        val isChord: Boolean = false
    )

    internal data class ParsedMeasure(
        val number: Int,
        val notes: List<ParsedNoteEvent> = emptyList(),
        val totalDivisions: Int = 1,
        val divisions: Int = 1,
        val tempo: Float? = null,
        val beats: Int = 4,
        val beatType: Int = 4,
        val repeatForward: Boolean = false,
        val repeatBackward: Boolean = false,
        val repeatTimes: Int = 2,
        val endingNumbers: List<Int> = emptyList()
    )

    internal data class TempNoteData(
        val step: String = "",
        val octave: Int = 4,
        val alter: Int = 0,
        val duration: Int = 1,
        val isRest: Boolean = false,
        val isChord: Boolean = false,
        val voice: Int = 1,
        val isTieStart: Boolean = false,
        val isTieStop: Boolean = false,
        val pitchShiftSemitones: Int = 0
    ) {
        fun toFrequency(): Float {
            if (isRest) return 0f
            val steps = mapOf("C" to 0, "D" to 2, "E" to 4, "F" to 5, "G" to 7, "A" to 9, "B" to 11)
            val baseNote = steps[step.uppercase()] ?: 0
            val writtenMidiNote = (octave + 1) * 12 + baseNote + alter
            val soundingMidiNote = writtenMidiNote + pitchShiftSemitones
            return 440f * 2.0f.pow((soundingMidiNote - 69) / 12.0f)
        }
    }

    private data class UnprocessedNote(
        val startTime: Int,
        val duration: Int,
        val frequency: Float,
        val partId: String,
        val voice: Int,
        val isTieStart: Boolean,
        val isTieStop: Boolean
    )

    private data class MutablePartInfo(
        val id: String,
        var name: String = "",
        var instrumentName: String = ""
    )

    fun parseSong(xmlString: String, songNameHint: String = ""): ParsedSong {
        val root = parseXmlTree(xmlString)
        var isSwingDetected = songNameHint.contains("swing", ignoreCase = true) ||
                xmlString.contains("swing", ignoreCase = true)

        var workTitle = ""
        var movementTitle = ""
        var composerName = ""

        val workTitleNodes = root.getElementsByTagName("work-title")
        if (workTitleNodes.isNotEmpty()) workTitle = workTitleNodes[0].textContent.trim()

        val movementTitleNodes = root.getElementsByTagName("movement-title")
        if (movementTitleNodes.isNotEmpty()) movementTitle = movementTitleNodes[0].textContent.trim()

        val creatorNodes = root.getElementsByTagName("creator")
        for (elem in creatorNodes) {
            if (elem.getAttribute("type") == "composer" || composerName.isEmpty()) {
                composerName = elem.textContent.trim()
            }
        }

        val declaredPartsMap = mutableMapOf<String, MutablePartInfo>()
        val partTranspositionMap = mutableMapOf<String, Int>()

        val scorePartNodes = root.getElementsByTagName("score-part")
        for (elem in scorePartNodes) {
            val pId = elem.getAttribute("id").ifEmpty { "P1" }
            var pName = ""
            var instName = ""
            val pn = elem.getElementsByTagName("part-name")
            if (pn.isNotEmpty()) pName = pn[0].textContent.trim()
            val iname = elem.getElementsByTagName("instrument-name")
            if (iname.isNotEmpty()) instName = iname[0].textContent.trim()
            declaredPartsMap[pId] = MutablePartInfo(pId, pName, instName)
        }

        val partMeasuresMap = mutableMapOf<String, MutableList<ParsedMeasure>>()
        val partNodes = root.getElementsByTagName("part")

        var currentPartDivisions = 1
        var currentPartBeats = 4
        var currentPartBeatType = 4
        var activeEndingNumbers: List<Int>

        for (pIdx in partNodes.indices) {
            val partElem = partNodes[pIdx]
            val pId = partElem.getAttribute("id").ifEmpty { "P1" }
            val currentPartMeasures = partMeasuresMap.getOrPut(pId) { mutableListOf() }
            activeEndingNumbers = emptyList()
            currentPartDivisions = 1
            currentPartBeats = 4
            currentPartBeatType = 4

            val measureNodes = partElem.children.filter { it.name.equals("measure", ignoreCase = true) }
            for (mIdx in measureNodes.indices) {
                val mElem = measureNodes[mIdx]
                val measureNum = mElem.getAttribute("number").toIntOrNull() ?: (mIdx + 1)

                var currentMeasureDivisions = 0
                var maxMeasureDivisions = 0
                var measureEndingNumbers = activeEndingNumbers
                var stopEndingAfterMeasure = false
                var measureRepeatForward = false
                var measureRepeatBackward = false
                var measureRepeatTimes = 2
                var explicitTempoInMeasure: Float? = null
                val measureNotes = mutableListOf<ParsedNoteEvent>()

                for (childElem in mElem.children) {
                    when (childElem.name.lowercase()) {
                        "attributes" -> {
                            val divs = childElem.getElementsByTagName("divisions")
                            if (divs.isNotEmpty()) {
                                currentPartDivisions = divs[0].textContent.trim().toIntOrNull()?.coerceAtLeast(1) ?: currentPartDivisions
                            }
                            val timeNodes = childElem.getElementsByTagName("time")
                            if (timeNodes.isNotEmpty()) {
                                val tElem = timeNodes[0]
                                val bNodes = tElem.getElementsByTagName("beats")
                                if (bNodes.isNotEmpty()) currentPartBeats = bNodes[0].textContent.trim().toIntOrNull()?.coerceAtLeast(1) ?: currentPartBeats
                                val btNodes = tElem.getElementsByTagName("beat-type")
                                if (btNodes.isNotEmpty()) currentPartBeatType = btNodes[0].textContent.trim().toIntOrNull()?.coerceAtLeast(1) ?: currentPartBeatType
                            }
                            val transposeNodes = childElem.getElementsByTagName("transpose")
                            if (transposeNodes.isNotEmpty()) {
                                val tElem = transposeNodes[0]
                                val chromNodes = tElem.getElementsByTagName("chromatic")
                                val octNodes = tElem.getElementsByTagName("octave-change")
                                val chrom = if (chromNodes.isNotEmpty()) chromNodes[0].textContent.trim().toIntOrNull() ?: 0 else 0
                                val octCh = if (octNodes.isNotEmpty()) octNodes[0].textContent.trim().toIntOrNull() ?: 0 else 0
                                partTranspositionMap[pId] = chrom + (octCh * 12)
                            }
                        }
                        "direction" -> {
                            val metronome = childElem.getElementsByTagName("metronome")
                            if (metronome.isNotEmpty()) {
                                val mEl = metronome[0]
                                val buNodes = mEl.getElementsByTagName("beat-unit")
                                val pmNodes = mEl.getElementsByTagName("per-minute")
                                val dotNodes = mEl.getElementsByTagName("beat-unit-dot")
                                if (pmNodes.isNotEmpty()) {
                                    val pmVal = pmNodes[0].textContent.trim().toFloatOrNull()
                                    if (pmVal != null) {
                                        val bUnit = if (buNodes.isNotEmpty()) buNodes[0].textContent.trim().lowercase() else "quarter"
                                        var mult = when (bUnit) {
                                            "long" -> 16.0f
                                            "breve" -> 8.0f
                                            "whole" -> 4.0f
                                            "half" -> 2.0f
                                            "quarter" -> 1.0f
                                            "eighth" -> 0.5f
                                            "16th" -> 0.25f
                                            "32nd" -> 0.125f
                                            "64th" -> 0.0625f
                                            else -> 1.0f
                                        }
                                        if (dotNodes.isNotEmpty()) mult *= 1.5f
                                        explicitTempoInMeasure = (pmVal * mult).coerceAtLeast(20f)
                                    }
                                }
                            } else {
                                val pm = childElem.getElementsByTagName("per-minute")
                                if (pm.isNotEmpty()) {
                                    explicitTempoInMeasure = pm[0].textContent.trim().toFloatOrNull()?.coerceAtLeast(20f)
                                }
                            }
                            val sound = childElem.getElementsByTagName("sound")
                            if (sound.isNotEmpty()) {
                                val tempoAttr = sound[0].getAttribute("tempo")
                                if (tempoAttr.isNotBlank()) {
                                    explicitTempoInMeasure = tempoAttr.toFloatOrNull()?.coerceAtLeast(20f)
                                }
                            }
                        }
                        "sound" -> {
                            val tempoAttr = childElem.getAttribute("tempo")
                            if (tempoAttr.isNotBlank()) {
                                explicitTempoInMeasure = tempoAttr.toFloatOrNull()?.coerceAtLeast(20f)
                            }
                        }
                        "barline" -> {
                            val repeats = childElem.getElementsByTagName("repeat")
                            if (repeats.isNotEmpty()) {
                                val dir = repeats[0].getAttribute("direction")
                                val times = repeats[0].getAttribute("times").toIntOrNull() ?: 2
                                if (dir == "forward") measureRepeatForward = true
                                if (dir == "backward") {
                                    measureRepeatBackward = true
                                    measureRepeatTimes = times
                                }
                            }
                            val endings = childElem.getElementsByTagName("ending")
                            if (endings.isNotEmpty()) {
                                val numAttr = endings[0].getAttribute("number")
                                val typeAttr = endings[0].getAttribute("type")
                                if (typeAttr == "start") {
                                    activeEndingNumbers = parseEndingNumbers(numAttr)
                                    measureEndingNumbers = activeEndingNumbers
                                } else if (typeAttr == "stop" || typeAttr == "discontinue") {
                                    stopEndingAfterMeasure = true
                                }
                            }
                        }
                        "note" -> {
                            var step = ""
                            var octave = 4
                            var alter = 0
                            var duration = 0
                            val isRest = childElem.getElementsByTagName("rest").isNotEmpty()
                            val isChord = childElem.getElementsByTagName("chord").isNotEmpty()
                            val voice = childElem.getElementsByTagName("voice").firstOrNull()?.textContent?.trim()?.toIntOrNull() ?: 1

                            var isTieStart = false
                            var isTieStop = false

                            val tieNodes = childElem.getElementsByTagName("tie") + childElem.getElementsByTagName("tied")
                            for (ti in tieNodes) {
                                val tAttr = ti.getAttribute("type")
                                if (tAttr == "start") isTieStart = true
                                if (tAttr == "stop") isTieStop = true
                            }

                            val pitchNodes = childElem.getElementsByTagName("pitch")
                            val unpitchedNodes = childElem.getElementsByTagName("unpitched")

                            if (pitchNodes.isNotEmpty()) {
                                val pElem = pitchNodes[0]
                                step = pElem.getElementsByTagName("step").firstOrNull()?.textContent?.trim() ?: ""
                                octave = pElem.getElementsByTagName("octave").firstOrNull()?.textContent?.trim()?.toIntOrNull() ?: 4
                                alter = pElem.getElementsByTagName("alter").firstOrNull()?.textContent?.trim()?.toIntOrNull() ?: 0
                            } else if (unpitchedNodes.isNotEmpty()) {
                                val uElem = unpitchedNodes[0]
                                step = uElem.getElementsByTagName("display-step").firstOrNull()?.textContent?.trim() ?: "C"
                                octave = uElem.getElementsByTagName("display-octave").firstOrNull()?.textContent?.trim()?.toIntOrNull() ?: 2
                            }

                            val durNodes = childElem.getElementsByTagName("duration")
                            if (durNodes.isNotEmpty()) {
                                duration = durNodes[0].textContent.trim().toIntOrNull() ?: 0
                            }

                            val pitchShift = partTranspositionMap[pId] ?: getFallbackTransposition(declaredPartsMap[pId]?.name ?: "", declaredPartsMap[pId]?.instrumentName ?: "")
                            val noteData = TempNoteData(step, octave, alter, duration, isRest, isChord, voice, isTieStart, isTieStop, pitchShift)
                            val startDivisions = if (isChord) {
                                (currentMeasureDivisions - duration).coerceAtLeast(0)
                            } else {
                                currentMeasureDivisions
                            }

                            measureNotes.add(
                                ParsedNoteEvent(
                                    relativeStartDivisions = startDivisions,
                                    durationDivisions = duration,
                                    noteData = noteData,
                                    isChord = isChord
                                )
                            )

                            if (!isChord) {
                                currentMeasureDivisions += duration
                            }
                            if (currentMeasureDivisions > maxMeasureDivisions) {
                                maxMeasureDivisions = currentMeasureDivisions
                            }
                        }
                        "backup" -> {
                            val durNodes = childElem.getElementsByTagName("duration")
                            val dur = if (durNodes.isNotEmpty()) durNodes[0].textContent.trim().toIntOrNull() ?: 0 else 0
                            currentMeasureDivisions = (currentMeasureDivisions - dur).coerceAtLeast(0)
                        }
                        "forward" -> {
                            val durNodes = childElem.getElementsByTagName("duration")
                            val dur = if (durNodes.isNotEmpty()) durNodes[0].textContent.trim().toIntOrNull() ?: 0 else 0
                            currentMeasureDivisions += dur
                            if (currentMeasureDivisions > maxMeasureDivisions) {
                                maxMeasureDivisions = currentMeasureDivisions
                            }
                        }
                    }
                }

                val parsedM = ParsedMeasure(
                    number = measureNum,
                    notes = measureNotes,
                    totalDivisions = maxMeasureDivisions,
                    divisions = currentPartDivisions,
                    tempo = explicitTempoInMeasure,
                    beats = currentPartBeats,
                    beatType = currentPartBeatType,
                    repeatForward = measureRepeatForward,
                    repeatBackward = measureRepeatBackward,
                    repeatTimes = measureRepeatTimes,
                    endingNumbers = measureEndingNumbers
                )
                currentPartMeasures.add(parsedM)

                if (stopEndingAfterMeasure) {
                    activeEndingNumbers = emptyList()
                }
            }
        }

        val unrolledMap = mutableMapOf<String, List<ParsedMeasure>>()
        for ((pId, rawMeasures) in partMeasuresMap) {
            unrolledMap[pId] = unrollMeasures(rawMeasures)
        }

        val (scoreTempo, measureStartMs) = calculateScoreMeasureTimelines(unrolledMap)

        val allNotes = mutableListOf<ParsedMusicNote>()
        for ((pId, unrolled) in unrolledMap) {
            val partRawNotes = mutableListOf<UnprocessedNote>()

            for (i in unrolled.indices) {
                val m = unrolled[i]
                val mStartMs = if (i < measureStartMs.size) measureStartMs[i] else 0
                val tempo = if (i < scoreTempo.size) scoreTempo[i] else 120.0f
                val msPerDiv = 60000f / tempo / m.divisions.coerceAtLeast(1)

                for (noteEvent in m.notes) {
                    val noteData = noteEvent.noteData ?: continue
                    if (noteData.isRest) continue

                    val startDivs = noteEvent.relativeStartDivisions
                    val noteStartMs = mStartMs + (startDivs * msPerDiv).toInt()
                    val noteDurationMs = (noteEvent.durationDivisions * msPerDiv).toInt()

                    var finalStartMs = noteStartMs
                    var finalDurationMs = noteDurationMs

                    if (isSwingDetected) {
                        val divisionsFloat = m.divisions.toFloat()
                        val startDivsFloat = startDivs.toFloat()
                        val durationDivsFloat = noteEvent.durationDivisions.toFloat()
                        val eighthDivisions = divisionsFloat / 2.0f

                        val beatOffset = (startDivsFloat % divisionsFloat + divisionsFloat) % divisionsFloat
                        val isDownbeat = abs(beatOffset) < 0.15f * divisionsFloat || abs(beatOffset - divisionsFloat) < 0.15f * divisionsFloat
                        val isUpbeat = abs(beatOffset - eighthDivisions) < 0.15f * divisionsFloat
                        val isEighthNote = abs(durationDivsFloat - eighthDivisions) < 0.2f * divisionsFloat

                        if (isEighthNote) {
                            if (isDownbeat) {
                                finalDurationMs = (noteDurationMs * 4.0f / 3.0f).toInt()
                            } else if (isUpbeat) {
                                val delayMs = (msPerDiv * divisionsFloat / 6.0f).toInt()
                                finalStartMs = noteStartMs + delayMs
                                finalDurationMs = (noteDurationMs * 2.0f / 3.0f).toInt()
                            }
                        } else if (isUpbeat) {
                            val delayMs = (msPerDiv * divisionsFloat / 6.0f).toInt()
                            finalStartMs = noteStartMs + delayMs
                        }
                    }

                    partRawNotes.add(
                        UnprocessedNote(
                            startTime = finalStartMs,
                            duration = finalDurationMs,
                            frequency = noteData.toFrequency(),
                            partId = pId,
                            voice = noteData.voice,
                            isTieStart = noteData.isTieStart,
                            isTieStop = noteData.isTieStop
                        )
                    )
                }
            }

            allNotes.addAll(processPartNotesAndMergeTies(partRawNotes))
        }

        val sortedNotes = allNotes.sortedBy { it.startTime }
        val noteCountsPerPart = sortedNotes.groupBy { it.partId }.mapValues { it.value.size }
        val partsList = mutableListOf<MusicPartInfo>()
        val allPartIds = (declaredPartsMap.keys + noteCountsPerPart.keys).distinct()

        for (pId in allPartIds) {
            val declared = declaredPartsMap[pId]
            val count = noteCountsPerPart[pId] ?: 0
            val name = declared?.name?.takeIf { it.isNotBlank() } ?: "Part $pId"
            val inst = declared?.instrumentName ?: ""
            partsList.add(MusicPartInfo(id = pId, name = name, instrumentName = inst, noteCount = count))
        }

        val resolvedTitle = when {
            workTitle.isNotBlank() -> workTitle
            movementTitle.isNotBlank() -> movementTitle
            songNameHint.isNotBlank() -> songNameHint.removeSuffix(".musicxml").removeSuffix(".xml")
            else -> "Untitled Song"
        }

        if (resolvedTitle.contains("swing", ignoreCase = true) || composerName.contains("swing", ignoreCase = true)) {
            isSwingDetected = true
        }

        val totalDurationMs = sortedNotes.maxOfOrNull { it.startTime + it.duration } ?: 0

        val metadata = SongMetadata(
            title = resolvedTitle,
            composer = composerName,
            parts = partsList,
            totalNotes = sortedNotes.size,
            totalDurationMs = totalDurationMs,
            isSwing = isSwingDetected
        )

        return ParsedSong(metadata = metadata, notes = sortedNotes)
    }

    private fun parseEndingNumbers(numberAttr: String): List<Int> {
        val regex = Regex("\\d+")
        val matches = regex.findAll(numberAttr).mapNotNull { it.value.toIntOrNull() }.toList()
        return matches.ifEmpty { listOf(1) }
    }

    private fun processPartNotesAndMergeTies(rawNotes: List<UnprocessedNote>): List<ParsedMusicNote> {
        val processedNotes = mutableListOf<ParsedMusicNote>()
        val notesByVoice = rawNotes.groupBy { it.voice }

        for ((_, voiceRawNotes) in notesByVoice) {
            val sortedRaw = voiceRawNotes.sortedBy { it.startTime }

            var currentNote: ParsedMusicNote? = null
            for (n in sortedRaw) {
                val note = ParsedMusicNote(
                    startTime = n.startTime,
                    duration = n.duration,
                    frequency = n.frequency,
                    partId = n.partId,
                    voice = n.voice,
                    isTieStart = n.isTieStart,
                    isTieStop = n.isTieStop
                )

                if (currentNote == null) {
                    currentNote = note
                    continue
                }

                val cur = currentNote
                val isSameFreq = abs(cur.frequency - note.frequency) < 0.1f
                val isConsecutive = note.startTime <= cur.startTime + cur.duration + 25
                val isTied = (cur.isTieStart || note.isTieStop) && isSameFreq && isConsecutive

                if (isTied) {
                    val combinedDuration = (note.startTime + note.duration) - cur.startTime
                    currentNote = cur.copy(
                        duration = combinedDuration,
                        isTieStart = note.isTieStart
                    )
                } else {
                    processedNotes.add(cur)
                    currentNote = note
                }
            }
            if (currentNote != null) {
                processedNotes.add(currentNote)
            }
        }

        return processedNotes
    }

    internal fun unrollMeasures(measures: List<ParsedMeasure>): List<ParsedMeasure> {
        if (measures.isEmpty()) return emptyList()

        val unrolled = mutableListOf<ParsedMeasure>()
        var i = 0
        var repeatStartIdx = 0
        var currentPass = 1
        var maxSafetyCounter = 0

        while (i < measures.size && maxSafetyCounter < 10000) {
            maxSafetyCounter++
            val m = measures[i]

            if (m.repeatForward) {
                repeatStartIdx = i
            }

            if (m.endingNumbers.isNotEmpty()) {
                if (!m.endingNumbers.contains(currentPass)) {
                    var nextIdx = i + 1
                    while (nextIdx < measures.size) {
                        val nextM = measures[nextIdx]
                        if (nextM.endingNumbers.isEmpty() || nextM.endingNumbers.contains(currentPass)) {
                            break
                        }
                        nextIdx++
                    }
                    i = nextIdx
                    continue
                }
            }

            unrolled.add(m)

            if (m.repeatBackward) {
                val targetPasses = m.repeatTimes.coerceAtLeast(2)
                if (currentPass < targetPasses) {
                    currentPass++
                    i = repeatStartIdx
                    continue
                } else {
                    currentPass = 1
                    repeatStartIdx = i + 1
                }
            }

            i++
        }

        return unrolled
    }

    private fun calculateScoreMeasureTimelines(
        unrolledMap: Map<String, List<ParsedMeasure>>
    ): Pair<FloatArray, IntArray> {
        val maxMeasures = unrolledMap.values.maxOfOrNull { it.size } ?: 0
        val scoreTempo = FloatArray(maxMeasures)
        val measureStartMs = IntArray(maxMeasures + 1)

        var currentTempo = 120.0f
        var currentBeats = 4
        var currentBeatType = 4

        for (i in 0 until maxMeasures) {
            var explicitTempoForMeasure: Float? = null

            for (measuresList in unrolledMap.values) {
                if (i < measuresList.size) {
                    val m = measuresList[i]
                    if (m.tempo != null) {
                        explicitTempoForMeasure = m.tempo
                    }
                    if (m.beats > 0 && m.beatType > 0) {
                        currentBeats = m.beats
                        currentBeatType = m.beatType
                    }
                }
            }

            if (explicitTempoForMeasure != null) {
                currentTempo = explicitTempoForMeasure
            }
            scoreTempo[i] = currentTempo

            var quarterBeats = currentBeats.toFloat() * (4.0f / currentBeatType.toFloat())

            for (measuresList in unrolledMap.values) {
                if (i < measuresList.size) {
                    val m = measuresList[i]
                    if (m.divisions > 0 && m.totalDivisions > 0) {
                        val noteQuarterBeats = m.totalDivisions.toFloat() / m.divisions.toFloat()
                        if (noteQuarterBeats > quarterBeats) {
                            quarterBeats = noteQuarterBeats
                        }
                    }
                }
            }

            val msPerQuarterBeat = 60000.0f / currentTempo
            val measureDurationMs = (quarterBeats * msPerQuarterBeat).toInt()

            measureStartMs[i + 1] = measureStartMs[i] + measureDurationMs
        }

        return Pair(scoreTempo, measureStartMs)
    }

    private fun parseXmlTree(xmlString: String): XmlNode {
        val rootChildren = mutableListOf<XmlNode>()
        val stack = mutableListOf<MutableNode>()

        var index = 0
        val len = xmlString.length

        while (index < len) {
            val openBracket = xmlString.indexOf('<', index)
            if (openBracket == -1) {
                if (stack.isNotEmpty()) {
                    val text = xmlString.substring(index).trim()
                    if (text.isNotEmpty()) {
                        stack.last().textBuilder.append(text)
                    }
                }
                break
            }

            if (openBracket > index && stack.isNotEmpty()) {
                val text = xmlString.substring(index, openBracket).trim()
                if (text.isNotEmpty()) {
                    stack.last().textBuilder.append(text).append(" ")
                }
            }

            val closeBracket = xmlString.indexOf('>', openBracket)
            if (closeBracket == -1) break

            val tagContent = xmlString.substring(openBracket + 1, closeBracket).trim()
            index = closeBracket + 1

            if (tagContent.startsWith("?") || tagContent.startsWith("!")) {
                continue
            }

            val isClosing = tagContent.startsWith("/")
            val isSelfClosing = tagContent.endsWith("/")

            val cleanContent = when {
                isClosing -> tagContent.substring(1).trim()
                isSelfClosing -> tagContent.substring(0, tagContent.length - 1).trim()
                else -> tagContent
            }

            val spaceIdx = cleanContent.indexOfFirst { it.isWhitespace() }
            val tagName = if (spaceIdx == -1) cleanContent else cleanContent.substring(0, spaceIdx)
            val attrStr = if (spaceIdx == -1) "" else cleanContent.substring(spaceIdx + 1)

            if (tagName.isBlank()) continue

            if (isClosing) {
                if (stack.isNotEmpty()) {
                    val pop = stack.removeAt(stack.lastIndex)
                    val built = XmlNode(
                        name = pop.name,
                        attributes = pop.attributes,
                        children = pop.children,
                        textContent = pop.textBuilder.toString().trim()
                    )
                    if (stack.isNotEmpty()) {
                        stack.last().children.add(built)
                    } else {
                        rootChildren.add(built)
                    }
                }
            } else {
                val attrs = mutableMapOf<String, String>()
                val attrMatches = Regex("(\\w+)=\"([^\"]*)\"").findAll(attrStr)
                for (am in attrMatches) {
                    attrs[am.groupValues[1]] = am.groupValues[2]
                }

                val newNode = MutableNode(name = tagName, attributes = attrs)

                if (isSelfClosing) {
                    val built = XmlNode(
                        name = newNode.name,
                        attributes = newNode.attributes,
                        children = emptyList(),
                        textContent = ""
                    )
                    if (stack.isNotEmpty()) {
                        stack.last().children.add(built)
                    } else {
                        rootChildren.add(built)
                    }
                } else {
                    stack.add(newNode)
                }
            }
        }

        return XmlNode(name = "root", children = rootChildren)
    }

    private class MutableNode(
        val name: String,
        val attributes: Map<String, String>,
        val children: MutableList<XmlNode> = mutableListOf(),
        val textBuilder: StringBuilder = StringBuilder()
    )

    companion object {
        fun getFallbackTransposition(partName: String, instrumentName: String): Int {
            val combined = "$partName $instrumentName".lowercase()
            return when {
                combined.contains("barytonhorn (diskantnøgle)") || combined.contains("baritone horn (treble") ||
                        combined.contains("tenorsax") || combined.contains("tenor sax") -> -14
                combined.contains("altsax") || combined.contains("alto sax") -> -9
                combined.contains("horn i f") || combined.contains("french horn") || combined.contains("corno") -> -7
                combined.contains("klarinet") || combined.contains("trompet") || combined.contains("kornet") ||
                        combined.contains("clarinet") || combined.contains("trumpet") || combined.contains("cornet") -> -2
                combined.contains("kontrabas") || combined.contains("double bass") || combined.contains("bass guitar") -> -12
                else -> 0
            }
        }
    }
}
