package com.vauth.foxyvpn.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.data.FxaAuthRepository
import com.vauth.foxyvpn.data.SessionStatus
import com.vauth.foxyvpn.ui.components.AuroraBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private const val SESSION_RESTORE_TIMEOUT_MS = 8_000L

@Composable
fun SplashScreen(
    authRepository: FxaAuthRepository,
    onSignedIn: () -> Unit,
    onNeedsLogin: () -> Unit,
) {
    LaunchedEffect(Unit) {
        delay(500)
        val status = withTimeoutOrNull(SESSION_RESTORE_TIMEOUT_MS) {
            authRepository.restoreSession()
        } ?: SessionStatus.UNREACHABLE

        when (status) {
            SessionStatus.ACTIVE -> onSignedIn()
            SessionStatus.UNREACHABLE -> onSignedIn()
            SessionStatus.NEEDS_LOGIN -> onNeedsLogin()
        }
    }

    val accent = MaterialTheme.colorScheme.primary
    val systemInDarkTheme = isSystemInDarkTheme()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AuroraBackdrop(
            accent = accent,
            support = MaterialTheme.colorScheme.tertiary,
            tertiary = MaterialTheme.colorScheme.secondary,
            intensity = if (systemInDarkTheme) 0.90f else 0.42f,
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BreathingShield(accent = accent)

            Spacer(Modifier.height(22.dp))

            Text(
                "FoxyVPN",
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Securing your connection",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))
            SweepingBar(accent = accent, modifier = Modifier.fillMaxWidth(0.36f))
        }
    }
}

@Composable
private fun BreathingShield(accent: Color) {
    val transition = rememberInfiniteTransition(label = "splash-shield")
    val breathe by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            tween(1_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "splash-shield-breathe",
    )
    val glow by transition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            tween(1_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "splash-shield-glow",
    )

    Box(
        modifier = Modifier
            .size(108.dp)
            .graphicsLayer {
                scaleX = breathe
                scaleY = breathe
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = glow), Color.Transparent),
                    center = center,
                    radius = size.minDimension / 2f,
                ),
                radius = size.minDimension / 2f,
                center = center,
            )
        }
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0.08f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

/** A thin track with a highlight that travels back and forth across it. */
@Composable
private fun SweepingBar(accent: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "splash-bar")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "splash-bar-progress",
    )
    val fade by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1_400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "splash-bar-fade",
    )

    Canvas(modifier.height(4.dp)) {
        val corner = CornerRadius(size.height / 2f, size.height / 2f)

        drawRoundRect(
            color = accent.copy(alpha = 0.15f),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = corner,
        )

        val headWidth = size.width * 0.38f
        val headLeft = (size.width - headWidth) * progress
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, accent, Color.Transparent),
                startX = headLeft,
                endX = headLeft + headWidth,
            ),
            topLeft = Offset(headLeft, 0f),
            size = Size(headWidth, size.height),
            cornerRadius = corner,
            alpha = if (fade < 0.5f) fade * 2f else (1f - fade) * 2f,
        )
    }
}
