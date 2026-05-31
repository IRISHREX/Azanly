package com.example.ui.shared

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun SoundWaveVisualizer(
    amplitude: Float,
    isLive: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF10B981) // Beautiful Emerald Green
) {
    // Continuous infinite tick for secondary micro-vibrations
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * java.lang.Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    // Smooth out incoming amplitude fluctuations using an organic bounciness spring
    val animatedAmplitude by animateFloatAsState(
        targetValue = amplitude,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "avgAmplitude"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val barCount = 28
        val spacing = 8.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(1f)
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            // Base height calculation depending on if stream is live or idle
            val waveFactor = if (isLive) {
                // Compose secondary oscillation per-bar using the continuous waveOffset and individual phase offsets
                val phase = (i.toFloat() / barCount) * 2f * java.lang.Math.PI.toFloat()
                val signal = sin(waveOffset + phase).coerceIn(-1.0f, 1.0f)
                
                // Scale bar heights based on the smooth animated audio amplitude
                val randomJitter = (i % 3) * 0.12f
                (animatedAmplitude * 0.65f) + (signal * 0.22f) + randomJitter
            } else {
                // Static, tranquil resting breathing lines in idle mode
                val restingPhase = (i.toFloat() / barCount) * java.lang.Math.PI.toFloat() * 1.5f
                0.08f + sin(restingPhase) * 0.03f
            }

            // Map and clamp raw height to canvas boundaries
            val clampedFactor = waveFactor.coerceIn(0.04f, 1.0f)
            val barHeight = (size.height * clampedFactor).coerceAtLeast(2f)

            // Draw symmetrically centered rounded bar shapes
            val x = i * (barWidth + spacing)
            val y = centerY - (barHeight / 2f)

            // Dynamic gradients representing live sound resonance
            val startColor = if (isLive) barColor else barColor.copy(alpha = 0.35f)
            val endColor = if (isLive) {
                // Merge into a beautiful amber/secondary accent matching user settings
                Color(0xFFD97706).copy(alpha = 0.85f)
            } else {
                barColor.copy(alpha = 0.15f)
            }

            drawRoundRect(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(startColor, endColor),
                    startY = y,
                    endY = y + barHeight
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
