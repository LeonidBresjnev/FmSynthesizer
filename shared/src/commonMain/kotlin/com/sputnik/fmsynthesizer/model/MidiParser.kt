package com.sputnik.fmsynthesizer.model

import kotlin.math.pow

class MidiParser {

    private class ByteReader(private val bytes: ByteArray) {
        var position = 0

        fun hasMore(): Boolean = position < bytes.size

        fun readByte(): Int {
            if (position >= bytes.size) return -1
            return bytes[position++].toInt() and 0xFF
        }

        fun readBytes(count: Int): ByteArray {
            val end = (position + count).coerceAtMost(bytes.size)
            val result = bytes.copyOfRange(position, end)
            position = end
            return result
        }

        fun readShort(): Int {
            val b1 = readByte()
            val b2 = readByte()
            return (b1 shl 8) or b2
        }

        fun readInt(): Int {
            val b1 = readByte()
            val b2 = readByte()
            val b3 = readByte()
            val b4 = readByte()
            return (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
        }

        fun readVLQ(): Int {
            var value = 0
            while (hasMore()) {
                val b = readByte()
                if (b < 0) break
                value = (value shl 7) or (b and 0x7F)
                if ((b and 0x80) == 0) break
            }
            return value
        }
    }

    private data class RawMidiEvent(
        val tick: Long,
        val trackIndex: Int,
        val status: Int,
        val channel: Int,
        val data1: Int,
        val data2: Int
    )

    private data class TempoEvent(
        val tick: Long,
        val usPerQuarter: Long
    )

    fun parseSong(content: String, songNameHint: String = ""): ParsedSong {
        return parseSong(content.toLatin1ByteArray(), songNameHint)
    }

    fun parseSong(bytes: ByteArray, songNameHint: String = ""): ParsedSong {
        if (bytes.size < 14) error("Invalid MIDI file: file too small")

        val reader = ByteReader(bytes)

        // Read MThd header
        val headerTag = bytesToStringLatin1(reader.readBytes(4))
        if (headerTag != "MThd") error("Invalid MIDI file: missing MThd header")

        val headerSize = reader.readInt()
        reader.readShort() // format (0, 1, or 2)
        val numTracks = reader.readShort()
        val division = reader.readShort()

        if (headerSize > 6) {
            reader.readBytes(headerSize - 6)
        }

        val ticksPerQuarter = if ((division and 0x8000) == 0) {
            division.coerceAtLeast(1)
        } else {
            val fps = (division shr 8) and 0xFF
            val ticksPerFrame = division and 0xFF
            (fps * ticksPerFrame).coerceAtLeast(1)
        }

        val allEvents = mutableListOf<RawMidiEvent>()
        val tempoEvents = mutableListOf<TempoEvent>()
        tempoEvents.add(TempoEvent(0L, 500_000L)) // Default 120 BPM

        val trackNames = mutableMapOf<Int, String>()
        val instrumentNames = mutableMapOf<Int, String>()
        val trackPrograms = mutableMapOf<Int, Int>()

        for (trackIdx in 0 until numTracks) {
            if (!reader.hasMore()) break
            val trackTag = bytesToStringLatin1(reader.readBytes(4))
            val trackLength = reader.readInt()
            val trackEndPos = reader.position + trackLength

            if (trackTag != "MTrk") {
                reader.position = trackEndPos
                continue
            }

            var currentTick = 0L
            var runningStatus = 0

            while (reader.position < trackEndPos && reader.hasMore()) {
                val deltaTime = reader.readVLQ()
                currentTick += deltaTime

                var status = reader.readByte()
                if (status < 0) break

                if (status < 0x80) {
                    if (runningStatus == 0) continue // Skip invalid state
                    reader.position--
                    status = runningStatus
                } else if (status < 0xF0) {
                    // Running status ONLY applies to channel messages (0x80..0xEF)
                    runningStatus = status
                }

                val command = status and 0xF0
                val channel = status and 0x0F

                when {
                    command == 0x80 -> { // Note Off
                        val note = reader.readByte()
                        val vel = reader.readByte()
                        allEvents.add(RawMidiEvent(currentTick, trackIdx, 0x80, channel, note, vel))
                    }
                    command == 0x90 -> { // Note On
                        val note = reader.readByte()
                        val vel = reader.readByte()
                        val actualCmd = if (vel == 0) 0x80 else 0x90
                        allEvents.add(RawMidiEvent(currentTick, trackIdx, actualCmd, channel, note, vel))
                    }
                    command == 0xA0 || command == 0xB0 || command == 0xE0 -> {
                        reader.readByte()
                        reader.readByte()
                    }
                    command == 0xC0 -> { // Program Change
                        val program = reader.readByte()
                        if (!trackPrograms.containsKey(trackIdx)) {
                            trackPrograms[trackIdx] = program
                        }
                    }
                    command == 0xD0 -> {
                        reader.readByte()
                    }
                    status == 0xF0 || status == 0xF7 -> { // SysEx
                        val len = reader.readVLQ()
                        reader.readBytes(len)
                    }
                    status == 0xFF -> { // Meta Event
                        val metaType = reader.readByte()
                        val len = reader.readVLQ()
                        val metaData = reader.readBytes(len)

                        if (metaType == 0x03) { // Sequence / Track Name
                            val name = bytesToStringLatin1(metaData).replace("\u0000", "").trim()
                            if (name.isNotEmpty()) {
                                trackNames[trackIdx] = name
                            }
                        } else if (metaType == 0x04) { // Instrument Name
                            val name = bytesToStringLatin1(metaData).replace("\u0000", "").trim()
                            if (name.isNotEmpty()) {
                                instrumentNames[trackIdx] = name
                            }
                        } else if (metaType == 0x51 && len >= 3) { // Set Tempo
                            val usPerQuarter = ((metaData[0].toInt() and 0xFF) shl 16) or
                                    ((metaData[1].toInt() and 0xFF) shl 8) or
                                    (metaData[2].toInt() and 0xFF)
                            tempoEvents.add(TempoEvent(currentTick, usPerQuarter.toLong()))
                        }
                    }
                }
            }
            reader.position = trackEndPos
        }

        tempoEvents.sortBy { it.tick }

        fun tickToMs(tick: Long): Int {
            var currentMs = 0.0
            var lastTick = 0L
            var currentUsPerQuarter = 500_000L

            for (te in tempoEvents) {
                if (tick <= te.tick) {
                    val deltaTicks = tick - lastTick
                    currentMs += (deltaTicks * currentUsPerQuarter) / (ticksPerQuarter * 1000.0)
                    return currentMs.toInt()
                } else {
                    val deltaTicks = te.tick - lastTick
                    currentMs += (deltaTicks * currentUsPerQuarter) / (ticksPerQuarter * 1000.0)
                    lastTick = te.tick
                    currentUsPerQuarter = te.usPerQuarter
                }
            }
            val deltaTicks = tick - lastTick
            currentMs += (deltaTicks * currentUsPerQuarter) / (ticksPerQuarter * 1000.0)
            return currentMs.toInt()
        }

        val activeNotes = mutableMapOf<Pair<Int, Int>, MutableList<Pair<Long, Int>>>()
        val parsedNotes = mutableListOf<ParsedMusicNote>()
        val partNoteCounts = mutableMapOf<Int, Int>()

        for (event in allEvents) {
            val key = Pair(event.trackIndex, event.data1)
            if (event.status == 0x90) {
                activeNotes.getOrPut(key) { mutableListOf() }.add(Pair(event.tick, event.data2))
            } else if (event.status == 0x80) {
                val list = activeNotes[key]
                if (!list.isNullOrEmpty()) {
                    val (startTick, _) = list.removeAt(0)
                    val startMs = tickToMs(startTick)
                    val endMs = tickToMs(event.tick)
                    val durationMs = (endMs - startMs).coerceAtLeast(50)

                    val midiNote = event.data1
                    val freq = 440f * 2.0f.pow((midiNote - 69) / 12.0f)

                    val partId = "P${event.trackIndex + 1}"
                    val voiceIdx = event.trackIndex

                    parsedNotes.add(
                        ParsedMusicNote(
                            startTime = startMs,
                            duration = durationMs,
                            frequency = freq,
                            partId = partId,
                            voice = voiceIdx
                        )
                    )
                    partNoteCounts[event.trackIndex] = (partNoteCounts[event.trackIndex] ?: 0) + 1
                }
            }
        }

        activeNotes.forEach { (key, list) ->
            val (trackIdx, note) = key
            for ((startTick, _) in list) {
                val startMs = tickToMs(startTick)
                val durationMs = 500
                val freq = 440f * 2.0f.pow((note - 69) / 12.0f)
                val partId = "P${trackIdx + 1}"
                parsedNotes.add(
                    ParsedMusicNote(
                        startTime = startMs,
                        duration = durationMs,
                        frequency = freq,
                        partId = partId,
                        voice = trackIdx
                    )
                )
                partNoteCounts[trackIdx] = (partNoteCounts[trackIdx] ?: 0) + 1
            }
        }

        parsedNotes.sortBy { it.startTime }

        val usedTracks = partNoteCounts.keys.sorted()
        val partsInfo = usedTracks.map { trackIdx ->
            val trackName = trackNames[trackIdx] ?: "Track ${trackIdx + 1}"
            val prog = trackPrograms[trackIdx] ?: 0
            val gmName = getGmInstrumentName(prog)
            val instName = instrumentNames[trackIdx] ?: gmName

            MusicPartInfo(
                id = "P${trackIdx + 1}",
                name = trackName,
                instrumentName = instName,
                noteCount = partNoteCounts[trackIdx] ?: 0
            )
        }

        val totalDurationMs = parsedNotes.maxOfOrNull { it.startTime + it.duration } ?: 0
        val songTitle = songNameHint.ifBlank { "MIDI Song" }

        val metadata = SongMetadata(
            title = songTitle,
            composer = "MIDI Import",
            parts = partsInfo,
            totalNotes = parsedNotes.size,
            totalDurationMs = totalDurationMs
        )

        return ParsedSong(metadata, parsedNotes)
    }

    private fun bytesToStringLatin1(bytes: ByteArray): String {
        val chars = CharArray(bytes.size) { i -> (bytes[i].toInt() and 0xFF).toChar() }
        return chars.concatToString()
    }

    private fun getGmInstrumentName(program: Int): String {
        val names = arrayOf(
            "Acoustic Grand Piano", "Bright Acoustic Piano", "Electric Grand Piano", "Honky-tonk Piano",
            "Electric Piano 1", "Electric Piano 2", "Harpsichord", "Clavinet",
            "Celesta", "Glockenspiel", "Music Box", "Vibraphone", "Marimba", "Xylophone", "Tubular Bells", "Dulcimer",
            "Drawbar Organ", "Percussive Organ", "Rock Organ", "Church Organ", "Reed Organ", "Accordion", "Harmonica", "Tango Accordion",
            "Acoustic Guitar (nylon)", "Acoustic Guitar (steel)", "Electric Guitar (jazz)", "Electric Guitar (clean)",
            "Electric Guitar (muted)", "Overdriven Guitar", "Distortion Guitar", "Guitar Harmonics",
            "Acoustic Bass", "Electric Bass (finger)", "Electric Bass (pick)", "Fretless Bass", "Slap Bass 1", "Slap Bass 2", "Synth Bass 1", "Synth Bass 2",
            "Violin", "Viola", "Cello", "Contrabass", "Tremolo Strings", "Pizzicato Strings", "Orchestral Harp", "Timpani",
            "String Ensemble 1", "String Ensemble 2", "Synth Strings 1", "Synth Strings 2", "Choir Aahs", "Voice Oohs", "Synth Voice", "Orchestra Hit",
            "Trumpet", "Trombone", "Tuba", "Muted Trumpet", "French Horn", "Brass Section", "Synth Brass 1", "Synth Brass 2",
            "Soprano Sax", "Alto Sax", "Tenor Sax", "Baritone Sax", "Oboe", "English Horn", "Bassoon", "Clarinet",
            "Piccolo", "Flute", "Recorder", "Pan Flute", "Blown Bottle", "Shakuhachi", "Whistle", "Ocarina",
            "Lead 1 (square)", "Lead 2 (sawtooth)", "Lead 3 (calliope)", "Lead 4 (chiff)", "Lead 5 (charang)", "Lead 6 (voice)", "Lead 7 (fifths)", "Lead 8 (bass + lead)",
            "Pad 1 (new age)", "Pad 2 (warm)", "Pad 3 (polysynth)", "Pad 4 (choir)", "Pad 5 (bowed)", "Pad 6 (metallic)", "Pad 7 (halo)", "Pad 8 (sweep)",
            "FX 1 (rain)", "FX 2 (soundtrack)", "FX 3 (crystal)", "FX 4 (atmosphere)", "FX 5 (brightness)", "FX 6 (goblins)", "FX 7 (echoes)", "FX 8 (sci-fi)",
            "Sitar", "Banjo", "Shamisen", "Koto", "Kalimba", "Bagpipe", "Fiddle", "Shanai", "Tinkle Bell", "Agogo", "Steel Drums", "Woodblock", "Taiko Drum", "Melodic Tom", "Synth Drum", "Reverse Cymbal", "Guitar Fret Noise", "Breath Noise", "Seashore", "Bird Tweet", "Telephone Ring", "Helicopter", "Applause", "Gunshot"
        )
        return names.getOrElse(program) { "MIDI Instrument $program" }
    }
}

fun String.toLatin1ByteArray(): ByteArray {
    val bytes = ByteArray(length)
    for (i in 0 until length) {
        bytes[i] = (this[i].code and 0xFF).toByte()
    }
    return bytes
}
