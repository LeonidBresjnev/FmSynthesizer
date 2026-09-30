package com.sputnik.fmsynthesizer.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sputnik.fmsynthesizer.model.SongDownloadState
import com.sputnik.fmsynthesizer.model.SongListState
import com.sputnik.fmsynthesizer.model.SynthesizerViewModel

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
