package com.sputnik.fmsynthesizer.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sputnik.fmsynthesizer.model.EnvelopeMode
import com.sputnik.fmsynthesizer.model.SynthesizerViewModel

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
