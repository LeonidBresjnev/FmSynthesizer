package com.sputnik.fmsynthesizer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sputnik.fmsynthesizer.model.ReverbPreset
import com.sputnik.fmsynthesizer.model.SynthesizerViewModel

@Composable
fun ReverbTab(viewModel: SynthesizerViewModel) {
    val isReverbEnabled by viewModel.isReverbEnabled.collectAsStateWithLifecycle()
    val selectedPreset by viewModel.selectedReverbPreset.collectAsStateWithLifecycle()
    val balance by viewModel.reverbBalance.collectAsStateWithLifecycle()
    val r by viewModel.reverbR.collectAsStateWithLifecycle()
    val g by viewModel.reverbG.collectAsStateWithLifecycle()
    val d by viewModel.reverbD.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Master Enable Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reverb Filter",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add spatial depth and room resonance to your music",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = isReverbEnabled,
                    onCheckedChange = { viewModel.toggleReverbEnabled(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Presets Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Reverb Presets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReverbPreset.entries.forEach { preset ->
                        val isSelected = selectedPreset == preset
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { viewModel.applyReverbPreset(preset) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.applyReverbPreset(preset) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = preset.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reverb Sliders Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .graphicsLayer { alpha = if (isReverbEnabled) 1f else 0.4f }
            ) {
                Text(
                    text = "Reverb Parameters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))

                ReverbSliderRow(
                    label = "Balance (Dry / Wet)",
                    value = balance,
                    enabled = isReverbEnabled,
                    onValueChange = { viewModel.setReverbBalance(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ReverbSliderRow(
                    label = "Feedback (R)",
                    value = r,
                    enabled = isReverbEnabled,
                    onValueChange = { viewModel.setReverbR(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ReverbSliderRow(
                    label = "High Frequency Damper (g)",
                    value = g,
                    enabled = isReverbEnabled,
                    onValueChange = { viewModel.setReverbG(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ReverbSliderRow(
                    label = "Diffusion (d)",
                    value = d,
                    enabled = isReverbEnabled,
                    onValueChange = { viewModel.setReverbD(it) }
                )
            }
        }
    }
}

@Composable
private fun ReverbSliderRow(
    label: String,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${(value * 100).toInt()}% (${(value * 100f) / 100f})",
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
