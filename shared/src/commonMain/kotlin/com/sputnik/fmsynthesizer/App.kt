package com.sputnik.fmsynthesizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sputnik.fmsynthesizer.model.EnvelopeMode
import com.sputnik.fmsynthesizer.model.SongDownloadState
import com.sputnik.fmsynthesizer.model.SongListState
import com.sputnik.fmsynthesizer.model.SynthesizerViewModel
import com.sputnik.fmsynthesizer.ui.EnvelopePlot
import com.sputnik.fmsynthesizer.ui.rememberFilePicker
import kotlinx.coroutines.launch

@Composable
fun App(viewModel: SynthesizerViewModel = remember { SynthesizerViewModel() }) {
    MaterialTheme {
        val pagerState = rememberPagerState(pageCount = { 3 })
        val coroutineScope = rememberCoroutineScope()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                SecondaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                        text = { Text("FM Settings") }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        text = { Text("Envelope") }
                    )
                    Tab(
                        selected = pagerState.currentPage == 2,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                        text = { Text("Music Library") }
                    )
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    when (page) {
                        0 -> FmSettingsTab(viewModel = viewModel)
                        1 -> EnvelopeTab(viewModel = viewModel)
                        2 -> MusicLibraryTab(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun FmSettingsTab(viewModel: SynthesizerViewModel) {
    val frequency by viewModel.frequency.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val index by viewModel.index.collectAsStateWithLifecycle()
    val carrierRatio by viewModel.carrierRatio.collectAsStateWithLifecycle()
    val index2 by viewModel.index2.collectAsStateWithLifecycle()
    val carrierRatio2 by viewModel.carrierRatio2.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "FM Synthesizer Controls",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.togglePlayStop() },
            modifier = Modifier.padding(8.dp)
        ) {
            Text(if (isPlaying) "Stop Test Tone" else "Play Test Tone")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Frequency: ${frequency.toInt()} Hz")
        Slider(
            value = frequency,
            onValueChange = { viewModel.setFrequency(it) },
            valueRange = 100f..2000f,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = "1st Order FM Modulation",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Modulation Index (I₁): ${((index * 100).toInt() / 100f)}")
        Slider(
            value = index,
            onValueChange = { viewModel.setModulationIndex(it) },
            valueRange = 0f..20f,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Carrier : Modulator Ratio (c:m) = ${carrierRatio.first} : ${carrierRatio.second}")
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Carrier (c)")
                WheelPicker(
                    value = carrierRatio.first,
                    range = 1..16,
                    onValueChange = { viewModel.setCMRatio(Pair(it, carrierRatio.second)) }
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Modulator (m)")
                WheelPicker(
                    value = carrierRatio.second,
                    range = 1..16,
                    onValueChange = { viewModel.setCMRatio(Pair(carrierRatio.first, it)) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = "2nd Order FM Modulation",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Modulation Index 2 (I₂): ${((index2 * 100).toInt() / 100f)}")
        Slider(
            value = index2,
            onValueChange = { viewModel.setModulationIndex2(it) },
            valueRange = 0f..20f,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Carrier2 : Modulator2 Ratio = ${carrierRatio2.first} : ${carrierRatio2.second}")
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Carrier 2")
                WheelPicker(
                    value = carrierRatio2.first,
                    range = 1..16,
                    onValueChange = { viewModel.setCMRatio2(Pair(it, carrierRatio2.second)) }
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Modulator 2")
                WheelPicker(
                    value = carrierRatio2.second,
                    range = 1..16,
                    onValueChange = { viewModel.setCMRatio2(Pair(carrierRatio2.first, it)) }
                )
            }
        }
    }
}

@Composable
fun WheelPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Button(
            onClick = {
                if (value > range.first) onValueChange(value - 1)
            },
            enabled = value > range.first,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text("-")
        }

        Box(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = value.toString(), fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = {
                if (value < range.last) onValueChange(value + 1)
            },
            enabled = value < range.last,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text("+")
        }
    }
}

@Composable
fun EnvelopeTab(viewModel: SynthesizerViewModel) {
    val envelopeMode by viewModel.envelopeMode.collectAsStateWithLifecycle()
    val attack by viewModel.attack.collectAsStateWithLifecycle()
    val decay by viewModel.decay.collectAsStateWithLifecycle()
    val sustain by viewModel.sustain.collectAsStateWithLifecycle()
    val release by viewModel.release.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Envelope Generator (ADSR)",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(220.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                EnvelopePlot(
                    attack = attack,
                    decay = decay,
                    sustain = sustain,
                    release = release,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Mode: ${envelopeMode.label}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = envelopeMode.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EnvelopeMode.entries.take(5).forEach { mode ->
                        Button(
                            onClick = { viewModel.setEnvelopeMode(mode) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(mode.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EnvelopeMode.entries.drop(5).forEach { mode ->
                        Button(
                            onClick = { viewModel.setEnvelopeMode(mode) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(mode.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Attack: ${((attack * 1000).toInt())} ms")
        Slider(
            value = attack,
            onValueChange = { viewModel.setAttack(it) },
            valueRange = 0.001f..2.0f,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Decay: ${((decay * 1000).toInt())} ms")
        Slider(
            value = decay,
            onValueChange = { viewModel.setDecay(it) },
            valueRange = 0.001f..2.0f,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Sustain Level: ${((sustain * 100).toInt())}%")
        Slider(
            value = sustain,
            onValueChange = { viewModel.setSustain(it) },
            valueRange = 0f..1.0f,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Release: ${((release * 1000).toInt())} ms")
        Slider(
            value = release,
            onValueChange = { viewModel.setRelease(it) },
            valueRange = 0.001f..2.0f,
            modifier = Modifier.fillMaxWidth(0.85f)
        )
    }
}

@Composable
fun MusicLibraryTab(viewModel: SynthesizerViewModel) {
    val songListState by viewModel.songListState.collectAsStateWithLifecycle()
    val selectedSong by viewModel.selectedSongItem.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val githubToken by viewModel.githubToken.collectAsStateWithLifecycle()
    val soloIndex by viewModel.soloInstrumentIndex.collectAsStateWithLifecycle()

    var songSourceSelected by remember { mutableStateOf(0) }
    var tokenInput by remember { mutableStateOf("") }
    val isTokenConfigured = githubToken.isNotBlank()

    val openFilePicker = rememberFilePicker { fileName, content ->
        viewModel.loadLocalMusicXml(fileName, content)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SecondaryTabRow(
            selectedTabIndex = songSourceSelected,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = songSourceSelected == 0,
                onClick = { songSourceSelected = 0 },
                text = { Text("GitHub Repository") }
            )
            Tab(
                selected = songSourceSelected == 1,
                onClick = { songSourceSelected = 1 },
                text = { Text("Local Files / Open") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (songSourceSelected == 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "https://github.com/LeonidBresjnev/myMusic.git",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { viewModel.fetchRemoteSongs() },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Refresh", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            label = {
                                Text(
                                    if (isTokenConfigured) "GitHub Token (Configured & Protected)" else "GitHub Personal Access Token",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            placeholder = {
                                if (isTokenConfigured) {
                                    Text("Token active. Enter new token to override.", style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (tokenInput.isNotBlank()) {
                                    viewModel.setGithubToken(tokenInput)
                                    tokenInput = ""
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(if (isTokenConfigured && tokenInput.isBlank()) "Token Saved" else "Save Token", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (val state = songListState) {
                        is SongListState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is SongListState.Error -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = state.message, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { viewModel.fetchRemoteSongs() }) {
                                    Text("Retry")
                                }
                            }
                        }
                        is SongListState.Success -> {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(state.songs) { song ->
                                    val isSelected = selectedSong?.downloadUrl == song.downloadUrl
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                            .clickable { viewModel.selectAndDownloadSong(song) }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = song.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "${song.size / 1024} KB",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Open Local MusicXML File",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select any .musicxml or .xml score file from your local disk or folder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { openFilePicker() }
                    ) {
                        Text("Open MusicXML File...")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Fixed Song Details Header + Scrollable Instrument List
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                when (val dlState = downloadState) {
                    is SongDownloadState.Idle -> {
                        Text(
                            text = "Song Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Select a song from GitHub repository or open a local MusicXML file.")
                    }
                    is SongDownloadState.Downloading -> {
                        Text(
                            text = "Song Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Parsing MusicXML score...")
                        }
                    }
                    is SongDownloadState.Error -> {
                        Text(
                            text = "Song Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Error: ${dlState.message}", color = MaterialTheme.colorScheme.error)
                    }
                    is SongDownloadState.Success -> {
                        val song = dlState.song
                        val meta = song.metadata

                        // Static Header Top (Fixed, does not scroll)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = meta.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Notes: ${meta.totalNotes} | Duration: ~${meta.totalDurationMs / 1000}s",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Button(
                                onClick = {
                                    if (isPlaying) {
                                        viewModel.stopSong()
                                    } else {
                                        viewModel.playSong(song)
                                    }
                                }
                            ) {
                                Text(if (isPlaying) "Stop Song" else "Play Song")
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Text(
                            text = "Instruments in Song (${meta.parts.size}):",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Scrollable Instrument List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            itemsIndexed(meta.parts) { index, part ->
                                val isEnabled = viewModel.isInstrumentEnabled(index)
                                val isSoloed = soloIndex == index
                                val cm = viewModel.getInstrumentCm(index)
                                val modIdx = viewModel.getInstrumentModIndex(index)
                                val envMode = viewModel.getInstrumentEnvelopeMode(index)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSoloed) MaterialTheme.colorScheme.tertiaryContainer
                                        else if (isEnabled) MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${part.id}: ${part.name.ifBlank { "Part ${index + 1}" }}",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            if (part.instrumentName.isNotBlank() && part.instrumentName != part.name) {
                                                Text(
                                                    text = "Instrument: ${part.instrumentName}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Badge { Text("c:m = ${cm.first}:${cm.second}") }
                                                Badge { Text("I = $modIdx") }
                                                Badge { Text("Mode = ${envMode.label}") }
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = { viewModel.toggleInstrumentEnabled(index) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(if (isEnabled) "ON" else "OFF", style = MaterialTheme.typography.labelSmall)
                                            }

                                            Button(
                                                onClick = { viewModel.toggleSoloInstrument(index) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSoloed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceTint
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("SOLO", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
