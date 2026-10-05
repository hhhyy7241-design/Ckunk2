package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BrandGradient
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SuccessGreen
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animación fluida e interactiva estilo Lottie para ilustraciones de descarga,
 * estados vacíos y elementos destacados.
 */
@Composable
fun LottieDownloadGraphic(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 120.dp,
    reducedMotion: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lottie_download")

    val pulse1 by if (reducedMotion) remember { mutableStateOf(1f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.45f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulse1"
        )
    }

    val alpha1 by if (reducedMotion) remember { mutableStateOf(0.25f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha1"
        )
    }

    val floatY by if (reducedMotion) remember { mutableStateOf(0f) } else {
        infiniteTransition.animateFloat(
            initialValue = -8f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "floatY"
        )
    }

    val rotationAngle by if (reducedMotion) remember { mutableStateOf(0f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
    }

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        // Ondas concéntricas de pulso estilo Lottie
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.32f

            // Anillo exterior que se expande
            drawCircle(
                color = ElectricCyan.copy(alpha = alpha1),
                radius = baseRadius * pulse1,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Anillo secundario desfasado
            val secondaryPulse = if (pulse1 > 1.1f) (pulse1 - 0.3f) else (pulse1 + 0.35f)
            val secondaryAlpha = (alpha1 * 0.7f).coerceIn(0f, 1f)
            drawCircle(
                color = ElectricBlue.copy(alpha = secondaryAlpha),
                radius = baseRadius * secondaryPulse,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Partículas flotantes que orbitan suavemente
            val particleCount = 6
            rotate(rotationAngle, pivot = center) {
                for (i in 0 until particleCount) {
                    val angle = (i * (360.0 / particleCount) * (Math.PI / 180.0)).toFloat()
                    val pDist = baseRadius * 1.35f
                    val px = center.x + pDist * cos(angle)
                    val py = center.y + pDist * sin(angle)
                    drawCircle(
                        color = if (i % 2 == 0) ElectricCyan else ElectricBlue,
                        radius = 2.5.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }
        }

        // Pod central flotante con sombra suave
        Box(
            modifier = Modifier
                .size(sizeDp * 0.58f)
                .graphicsLayer { translationY = floatY }
                .shadow(16.dp, CircleShape, ambientColor = ElectricBlue, spotColor = ElectricCyan)
                .clip(CircleShape)
                .background(BrandGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(sizeDp * 0.32f)
            )
        }
    }
}

/**
 * Animación estilo Lottie para celebración o cuando una descarga se completa
 */
@Composable
fun LottieCelebrationAnimation(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 100.dp,
    reducedMotion: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lottie_celebrate")

    val pulse by if (reducedMotion) remember { mutableStateOf(1f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "celebrate_pulse"
        )
    }

    val spin by if (reducedMotion) remember { mutableStateOf(0f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "celebrate_spin"
        )
    }

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.42f

            // Partículas de confeti radial
            rotate(spin, pivot = center) {
                for (i in 0 until 8) {
                    val angle = (i * 45.0 * (Math.PI / 180.0)).toFloat()
                    val dist = radius * pulse
                    val cx = center.x + dist * cos(angle)
                    val cy = center.y + dist * sin(angle)
                    val color = when (i % 3) {
                        0 -> SuccessGreen
                        1 -> ElectricCyan
                        else -> ElectricBlue
                    }
                    drawCircle(color = color, radius = 3.dp.toPx(), center = Offset(cx, cy))
                }
            }
        }

        Box(
            modifier = Modifier
                .size(sizeDp * 0.52f)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .shadow(12.dp, CircleShape, ambientColor = SuccessGreen, spotColor = SuccessGreen)
                .clip(CircleShape)
                .background(SuccessGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(sizeDp * 0.28f)
            )
        }
    }
}

/**
 * Animación de escaneo Lottie para la validación de enlaces y códigos Moodle
 */
@Composable
fun LottieScannerBeam(
    modifier: Modifier = Modifier,
    heightDp: Dp = 4.dp,
    reducedMotion: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lottie_scanner")

    val sweepFraction by if (reducedMotion) remember { mutableStateOf(0.5f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scanner_sweep"
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
    ) {
        val width = size.width
        val beamWidth = width * 0.35f
        val startX = (width - beamWidth) * sweepFraction

        val brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                ElectricCyan.copy(alpha = 0.8f),
                ElectricBlue,
                ElectricCyan.copy(alpha = 0.8f),
                Color.Transparent
            ),
            startX = startX,
            endX = startX + beamWidth
        )

        drawRoundRect(
            brush = brush,
            topLeft = Offset(startX, 0f),
            size = Size(beamWidth, size.height),
            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
        )
    }
}

/**
 * Animación de ecualizador de paquetes/chunks descargando en tiempo real
 */
@Composable
fun LottieEqualizerChunks(
    modifier: Modifier = Modifier,
    barCount: Int = 5,
    reducedMotion: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lottie_equalizer")

    val anim1 by if (reducedMotion) remember { mutableStateOf(0.5f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eq1"
        )
    }

    val anim2 by if (reducedMotion) remember { mutableStateOf(0.7f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eq2"
        )
    }

    val anim3 by if (reducedMotion) remember { mutableStateOf(0.4f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eq3"
        )
    }

    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val barWidth = (totalWidth / (barCount * 2 - 1)).coerceAtLeast(3f)
        val gap = barWidth

        for (i in 0 until barCount) {
            val factor = when (i % 3) {
                0 -> anim1
                1 -> anim2
                else -> anim3
            }
            val barHeight = (size.height * factor).coerceAtLeast(4f)
            val x = i * (barWidth + gap)
            val y = size.height - barHeight

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(ElectricCyan, ElectricBlue),
                    startY = y,
                    endY = size.height
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
