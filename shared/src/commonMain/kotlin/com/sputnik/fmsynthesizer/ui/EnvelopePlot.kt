package com.sputnik.fmsynthesizer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

@Composable
fun EnvelopePlot(
    attack: Float,
    decay: Float,
    sustain: Float,
    release: Float,
    modifier: Modifier = Modifier
) {
    val attackMs = (attack * 1000f).coerceAtLeast(1f)
    val decayMs = (decay * 1000f).coerceAtLeast(1f)
    val sustainMs = 500f
    val sustainLevel = sustain.coerceIn(0f, 1f)
    val releaseMs = (release * 1000f).coerceAtLeast(1f)

    val totalMs = attackMs + decayMs + sustainMs + releaseMs

    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val paddingLeft = 40f
            val paddingBottom = 30f
            val chartWidth = (width - paddingLeft).coerceAtLeast(10f)
            val chartHeight = (height - paddingBottom).coerceAtLeast(10f)

            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            for (step in 1..3) {
                val y = chartHeight * (1f - step / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(width, y),
                    pathEffect = dashEffect
                )
            }

            val attackX = paddingLeft + (attackMs / totalMs) * chartWidth
            val decayX = paddingLeft + ((attackMs + decayMs) / totalMs) * chartWidth
            val sustainX = paddingLeft + ((attackMs + decayMs + sustainMs) / totalMs) * chartWidth
            val releaseX = paddingLeft + chartWidth

            drawLine(color = gridColor, start = Offset(attackX, 0f), end = Offset(attackX, chartHeight), pathEffect = dashEffect)
            drawLine(color = gridColor, start = Offset(decayX, 0f), end = Offset(decayX, chartHeight), pathEffect = dashEffect)
            drawLine(color = gridColor, start = Offset(sustainX, 0f), end = Offset(sustainX, chartHeight), pathEffect = dashEffect)

            val path = Path().apply {
                moveTo(paddingLeft, chartHeight)
                quadraticTo(
                    paddingLeft + (attackX - paddingLeft) * 0.5f,
                    chartHeight,
                    attackX,
                    0f
                )
                lineTo(decayX, chartHeight * (1f - sustainLevel))
                lineTo(sustainX, chartHeight * (1f - sustainLevel))
                lineTo(releaseX, chartHeight)
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 4f)
            )

            drawLine(color = textColor, start = Offset(paddingLeft, 0f), end = Offset(paddingLeft, chartHeight), strokeWidth = 2f)
            drawLine(color = textColor, start = Offset(paddingLeft, chartHeight), end = Offset(width, chartHeight), strokeWidth = 2f)

            drawText(textMeasurer, "1.0", Offset(0f, 0f))
            drawText(textMeasurer, "0.0", Offset(0f, chartHeight - 15f))
            drawText(textMeasurer, "Attack", Offset((paddingLeft + attackX) / 2f - 20f, chartHeight + 5f))
            drawText(textMeasurer, "Decay", Offset((attackX + decayX) / 2f - 18f, chartHeight + 5f))
            drawText(textMeasurer, "Sustain", Offset((decayX + sustainX) / 2f - 22f, chartHeight + 5f))
            drawText(textMeasurer, "Release", Offset((sustainX + releaseX) / 2f - 22f, chartHeight + 5f))
        }
    }
}
