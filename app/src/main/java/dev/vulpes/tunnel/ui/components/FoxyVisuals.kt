package dev.vulpes.tunnel.ui.components

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vulpes.tunnel.data.model.ConnectionState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val TAU = (2.0 * PI).toFloat()

/**
 * A slow-drifting field of coloured light that sits behind a screen.
 *
 * Three radial blobs orbit on different phases and one shared "breath" scales them all, so the
 * backdrop never looks static but also never distracts from the content on top of it.
 *
 * @param intensity multiplier on every blob's alpha. Drop it for light themes, where a full
 *   strength glow would wash out the text.
 */
@Composable
fun AuroraBackdrop(
    accent: Color,
    support: Color,
    tertiary: Color,
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = TAU,
        animationSpec = infiniteRepeatable(tween(20_000, easing = LinearEasing)),
        label = "aurora-drift",
    )
    val breathe by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            tween(7_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aurora-breathe",
    )

    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                val w = size.width
                val h = size.height
                val radius = minOf(w, h) * 0.72f * breathe

                // Base wash, so the blobs blend into the surface instead of floating on it.
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(accent.copy(alpha = 0.10f * intensity), Color.Transparent),
                        startY = 0f,
                        endY = h,
                    ),
                )

                val firstCenter = Offset(w * (0.20f + 0.16f * cos(drift)), h * (0.18f + 0.09f * sin(drift)))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.38f * intensity), Color.Transparent),
                        center = firstCenter,
                        radius = radius,
                    ),
                    radius = radius,
                    center = firstCenter,
                )

                val secondCenter = Offset(
                    w * (0.82f + 0.12f * cos(drift + 2.1f)),
                    h * (0.30f + 0.13f * sin(drift + 2.1f)),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(support.copy(alpha = 0.30f * intensity), Color.Transparent),
                        center = secondCenter,
                        radius = radius * 0.88f,
                    ),
                    radius = radius * 0.88f,
                    center = secondCenter,
                )

                val thirdCenter = Offset(
                    w * (0.45f + 0.20f * sin(drift * 1.35f)),
                    h * (0.86f + 0.07f * cos(drift * 1.35f)),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tertiary.copy(alpha = 0.26f * intensity), Color.Transparent),
                        center = thirdCenter,
                        radius = radius * 0.95f,
                    ),
                    radius = radius * 0.95f,
                    center = thirdCenter,
                )
            },
    )
}

/**
 * The connect/disconnect control.
 *
 * The power glyph is drawn by hand rather than taken from the icon set so it can animate: the ring
 * draws itself on while connecting and the stem grows into place, then three motes orbit the rim
 * once the tunnel is live. DISCONNECTED shows only the track, so the state reads at a glance
 * without relying on the label underneath.
 */
@Composable
fun PowerOrb(
    state: ConnectionState,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 196.dp,
    connectLabel: String,
    disconnectLabel: String,
    onToggle: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "orb")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1_500, easing = LinearEasing)),
        label = "orb-sweep",
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = LinearEasing)),
        label = "orb-pulse",
    )

    // One shared driver for the "draw the glyph on" effect.
    val drawOn by animateFloatAsState(
        targetValue = if (state == ConnectionState.DISCONNECTED) 0f else 1f,
        animationSpec = tween(if (state == ConnectionState.CONNECTING) 700 else 420),
        label = "orb-drawon",
    )

    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "orb-press",
    )

    val connected = state == ConnectionState.CONNECTED
    val haloAlpha = if (connected) 0.18f + 0.20f * pulse else 0.08f
    val trackWidth = with(LocalDensity.current) { 10.dp.toPx() }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onToggle() },
                )
            }
            .semantics {
                contentDescription = if (connected) disconnectLabel else connectLabel
                onClick(label = if (connected) disconnectLabel else connectLabel) {
                    onToggle()
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val inset = trackWidth / 2f
            val arcSize = Size(this.size.width - trackWidth, this.size.height - trackWidth)
            val topLeft = Offset(inset, inset)
            val half = this.size.minDimension / 2f

            // Breathing halo.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = haloAlpha), Color.Transparent),
                    center = center,
                    radius = half,
                ),
                radius = half,
                center = center,
            )

            // Idle track.
            drawArc(
                color = accent.copy(alpha = 0.13f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = trackWidth, cap = StrokeCap.Round),
            )

            when (state) {
                ConnectionState.CONNECTING -> {
                    // A comet with a fading tail rather than a flat arc.
                    for (segment in 0 until 5) {
                        drawArc(
                            color = accent.copy(alpha = 0.85f - segment * 0.16f),
                            startAngle = sweep - segment * 20f,
                            sweepAngle = 20f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = trackWidth, cap = StrokeCap.Round),
                        )
                    }
                }

                ConnectionState.CONNECTED -> {
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = trackWidth, cap = StrokeCap.Round),
                    )
                    // Three motes orbiting the rim, evenly spaced.
                    val orbit = this.size.minDimension / 2f - trackWidth / 2f
                    for (i in 0 until 3) {
                        val angle = Math.toRadians((sweep / 2f + i * 120f).toDouble())
                        drawCircle(
                            color = accent.copy(alpha = 0.55f + 0.35f * pulse),
                            radius = trackWidth * 0.30f,
                            center = Offset(
                                x = center.x + (orbit * kotlin.math.cos(angle)).toFloat(),
                                y = center.y + (orbit * kotlin.math.sin(angle)).toFloat(),
                            ),
                        )
                    }
                }

                ConnectionState.DISCONNECTED -> Unit
            }
        }

        Box(
            modifier = Modifier
                .size(size * 0.64f)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accent.copy(alpha = if (connected) 0.30f else 0.18f),
                            accent.copy(alpha = 0.06f),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            PowerGlyph(
                accent = accent,
                progress = drawOn,
                dim = !connected,
                size = size * 0.25f,
            )
        }
    }
}

/**
 * The standard power symbol: a ring open at the top with a stem through the gap.
 *
 * @param progress 0 draws nothing, 1 draws the full glyph. Used for the draw-on effect.
 */
@Composable
private fun PowerGlyph(
    accent: Color,
    progress: Float,
    dim: Boolean,
    size: Dp,
) {
    val gapDegrees = 62f
    val stemStroke = with(LocalDensity.current) { 7.dp.toPx() }

    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // The ring occupies the lower ~72% so the stem has room to stand proud of it.
        val ringDiameter = w * 0.78f
        val ringLeft = (w - ringDiameter) / 2f
        val ringTop = h - ringDiameter
        val ringSize = Size(ringDiameter, ringDiameter)

        val ringSweep = (360f - gapDegrees) * progress
        drawArc(
            color = accent.copy(alpha = if (dim) 0.55f else 1f),
            startAngle = -90f + gapDegrees / 2f,
            sweepAngle = ringSweep,
            useCenter = false,
            topLeft = Offset(ringLeft, ringTop),
            size = ringSize,
            style = Stroke(width = stemStroke, cap = StrokeCap.Round),
        )

        val stemBottom = ringTop + ringDiameter * 0.42f
        val stemTop = stemBottom - (ringDiameter * 0.52f) * progress
        drawLine(
            color = accent.copy(alpha = if (dim) 0.55f else 1f),
            start = Offset(w / 2f, stemBottom),
            end = Offset(w / 2f, stemTop),
            strokeWidth = stemStroke,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * A translucent card with a hairline border.
 *
 * Deliberately avoids `Modifier.blur`, which needs API 31 while this app supports down to API 26.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Card(
        modifier = modifier.then(clickModifier),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.68f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** A small pill used for the status readout above the orb. */
@Composable
fun StatusPill(
    text: String,
    dotColor: Color,
    pulsing: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "pill")
    val dotPulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pill-pulse",
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.70f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = if (pulsing) dotPulse else 1f)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** One label/value pair inside a stats row. */
@Composable
fun StatBlock(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = accent,
        )
        Spacer(Modifier.size(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
