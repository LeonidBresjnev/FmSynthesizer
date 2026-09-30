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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sputnik.fmsynthesizer.model.EnvelopeMode
import com.sputnik.fmsynthesizer.model.SongDownloadState
import com.sputnik.fmsynthesizer.model.SongListState
import com.sputnik.fmsynthesizer.model.SynthesizerViewModel
import com.sputnik.fmsynthesizer.ui.EnvelopePlot

@Composable
fun WheelPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange = 1..15,
    label: String,
    modifier: Modifier = Modifier
) {
    val items = remember(range) { range.toList() }
    val initialPage = (value - range.first).coerceIn(0, items.size - 1)
    val pagerState = rememberPagerState(initialPage = initialPage) { items.size }

    LaunchedEffect(pagerState.currentPage) {
        val selectedValue = items[pagerState.currentPage]
        if (selectedValue != value) {
            onValueChange(selectedValue)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 32.dp)
            ) { page ->
                val item = items[page]
                val isSelected = page == pagerState.currentPage
                Text(
                    text = item.toString(),
                    style = if (isSelected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(36.dp)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
            )
        }
    }
}

@Composable
@Preview
fun App(viewModel: SynthesizerViewModel = viewModel { SynthesizerViewModel() }) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("FM Settings", "Envelope", "Music Library")

    MaterialTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize()
        ) {
            SecondaryTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(text = title) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> FmSettingsTab(viewModel)
                1 -> EnvelopeTab(viewModel)
                2 -> MusicLibraryTab(viewModel)
            }
        }
    }
}

@Composable
fun FmSettingsTab(viewModel: SynthesizerViewModel) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val frequency by viewModel.frequency.collectAsStateWithLifecycle()
    val modulationIndex by viewModel.index.collectAsStateWithLifecycle()
    val cmRatio by viewModel.carrierRatio.collectAsStateWithLifecycle()

    val modulationIndex2 by viewModel.index2.collectAsStateWithLifecycle()
    val cmRatio2 by viewModel.carrierRatio2.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { viewModel.togglePlayStop() }
        ) {
            Text(if (isPlaying) "Stop" else "Play")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Fundamental Frequency: ${frequency.toInt()} Hz",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Spacer(modifier = Modifier.height(6.dp))

        Slider(
            value = frequency,
            onValueChange = { viewModel.setFrequency(it) },
            valueRange = 55f..1760f,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.fillMaxWidth(0.9f))
        Spacer(modifier = Modifier.height(16.dp))

        // 1st Order FM Controls
        Text(
            text = "1st Order Modulation Index: ${((modulationIndex * 100).toInt() / 100f)}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Spacer(modifier = Modifier.height(6.dp))

        Slider(
            value = modulationIndex,
            onValueChange = { viewModel.setModulationIndex(it) },
            valueRange = 0f..20f,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "1st Order Ratio (${cmRatio.first} : ${cmRatio.second})",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPicker(
                value = cmRatio.first,
                onValueChange = { viewModel.setCMRatio(Pair(it, cmRatio.second)) },
                range = 1..15,
                label = "Carrier 1"
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            WheelPicker(
                value = cmRatio.second,
                onValueChange = { viewModel.setCMRatio(Pair(cmRatio.first, it)) },
                range = 1..15,
                label = "Modulator 1"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(modifier = Modifier.fillMaxWidth(0.9f))
        Spacer(modifier = Modifier.height(16.dp))

        // 2nd Order FM Controls
        Text(
            text = "2nd Order Modulation Index: ${((modulationIndex2 * 100).toInt() / 100f)}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Spacer(modifier = Modifier.height(6.dp))

        Slider(
            value = modulationIndex2,
            onValueChange = { viewModel.setModulationIndex2(it) },
            valueRange = 0f..20f,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "2nd Order Ratio (${cmRatio2.first} : ${cmRatio2.second})",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPicker(
                value = cmRatio2.first,
                onValueChange = { viewModel.setCMRatio2(Pair(it, cmRatio2.second)) },
                range = 1..15,
                label = "Carrier 2"
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            WheelPicker(
                value = cmRatio2.second,
                onValueChange = { viewModel.setCMRatio2(Pair(cmRatio2.first, it)) },
                range = 1..15,
                label = "Modulator 2"
            )
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Envelope Generator",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

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
    val stateVersion by viewModel.instrumentStateVersion.collectAsStateWithLifecycle()

    var tokenInput by remember { mutableStateOf("") }
    val isTokenConfigured = githubToken.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "GitHub Music Repository",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "https://github.com/LeonidBresjnev/myMusic.git",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = { viewModel.fetchRemoteSongs() }) {
                Text("Refresh")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                        if (isTokenConfigured) "GitHub Token (Configured & Protected)" else "GitHub Personal Access Token"
                    )
                },
                placeholder = {
                    if (isTokenConfigured) {
                        Text("Token active and protected. Enter new token to override.")
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
                }
            ) {
                Text(if (isTokenConfigured && tokenInput.isBlank()) "Token Saved" else "Save Token")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Song Library",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
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
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = song.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Song Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                when (val dlState = downloadState) {
                    is SongDownloadState.Idle -> {
                        Text("Select a song above to load metadata and notes.")
                    }
                    is SongDownloadState.Downloading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Parsing MusicXML...")
                        }
                    }
                    is SongDownloadState.Error -> {
                        Text("Error: ${dlState.message}", color = MaterialTheme.colorScheme.error)
                    }
                    is SongDownloadState.Success -> {
                        val song = dlState.song
                        val meta = song.metadata

                        Text(
                            text = meta.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(text = "Total Notes: ${meta.totalNotes} | Duration: ${meta.totalDurationMs / 1000}s")

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Instruments in Song (${meta.parts.size}):",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        meta.parts.forEachIndexed { index, part ->
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
                                        .padding(12.dp),
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
