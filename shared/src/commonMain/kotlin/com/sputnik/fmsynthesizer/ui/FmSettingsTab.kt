package com.sputnik.fmsynthesizer.ui

import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import com.sputnik.fmsynthesizer.model.SynthesizerViewModel

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
            valueRange = 50f..2000f,
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
