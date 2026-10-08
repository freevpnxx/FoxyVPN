package dev.vulpes.tunnel.ui.components

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.vulpes.tunnel.R
import dev.vulpes.tunnel.data.formatBytes
import dev.vulpes.tunnel.data.formatBytesPrecise
import dev.vulpes.tunnel.data.formatDuration
import dev.vulpes.tunnel.data.formatRoughSpan
import dev.vulpes.tunnel.vpn.SessionStats
import kotlinx.coroutines.delay

/** Re-emits every second while [active], so duration readouts tick without a manual timer. */
@Composable
fun rememberTickingElapsed(startedAtElapsedMs: Long?, active: Boolean): Long {
    var now by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(startedAtElapsedMs, active) {
        if (!active || startedAtElapsedMs == null) return@LaunchedEffect
        while (true) {
            now = android.os.SystemClock.elapsedRealtime()
            delay(1_000)
        }
    }
    return startedAtElapsedMs?.let { (now - it).coerceAtLeast(0L) } ?: 0L
}

/**
 * Remaining data allowance.
 *
 * The figure is [SessionStats.liveQuotaRemaining], i.e. the allowance the proxy pass reported at
 * connect time minus every byte this session has counted since, so it keeps falling as the user
 * downloads instead of sitting at its stale connect-time value.
 */
@Composable
fun QuotaSection(
    stats: SessionStats,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val remaining = stats.liveQuotaRemaining
    val max = stats.quotaMax
    val used = if (remaining != null && max != null) (max - remaining).coerceAtLeast(0L) else null
    val fraction = if (used != null && max != null && max > 0) {
        (used.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 700),
        label = "quota-fraction",
    )
    val barColor by animateColorAsState(
        targetValue = when {
            fraction > 0.85f -> MaterialTheme.colorScheme.error
            fraction > 0.65f -> MaterialTheme.colorScheme.tertiary
            else -> accent
        },
        label = "quota-color",
    )

    val resetLabel = stats.quotaResetEpochSeconds?.let { resetEpoch ->
        val remainingMs = resetEpoch * 1_000L - System.currentTimeMillis()
        if (remainingMs > 0) {
            stringResource(R.string.quota_resets_in, formatRoughSpan(remainingMs))
        } else {
            null
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(barColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (stats.quotaUnlimited) {
                        Icons.Filled.AllInclusive
                    } else {
                        Icons.Filled.Bolt
                    },
                    contentDescription = null,
                    tint = barColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.quota_section),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            if (!stats.quotaUnlimited && max != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(barColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        "${(fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = barColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        when {
            stats.quotaUnlimited -> {
                Text(
                    stringResource(R.string.quota_unlimited),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.quota_unlimited_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            remaining == null || max == null -> {
                Text(
                    stringResource(R.string.quota_unavailable),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.quota_unavailable_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        formatBytesPrecise(remaining),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = barColor,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.quota_left_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }

                Spacer(Modifier.height(12.dp))
                QuotaBar(fraction = animatedFraction, color = barColor)

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(
                            R.string.quota_used_of,
                            formatBytes(used ?: 0L),
                            formatBytes(max),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (resetLabel != null) {
                        Text(
                            resetLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}


/** A rounded, gradient-filled meter drawn by hand so the corners and cap stay crisp. */
@Composable
private fun QuotaBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier.fillMaxWidth().height(10.dp)) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(color = track, cornerRadius = radius)
        val width = size.width * fraction
        if (width > 1f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(color.copy(alpha = 0.65f), color),
                    startX = 0f,
                    endX = width,
                ),
                topLeft = Offset.Zero,
                size = Size(width, size.height),
                cornerRadius = radius,
            )
        }
    }
}

/** One icon + label + value cell used inside the session strip. */
@Composable
private fun SessionCell(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Duration, download, upload and exit node in one strip.
 *
 * [elapsedMs] is passed in rather than read from [stats] so the caller can drive the one-second
 * tick from a single place.
 */
@Composable
fun SessionSection(
    stats: SessionStats,
    elapsedMs: Long,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            SessionCell(
                icon = Icons.Filled.Schedule,
                label = stringResource(R.string.session_duration),
                value = formatDuration(elapsedMs),
                tint = accent,
                modifier = Modifier.weight(1f),
            )
            CellDivider()
            SessionCell(
                icon = Icons.Filled.ArrowDownward,
                label = stringResource(R.string.session_download),
                value = formatBytes(stats.downBytesTotal),
                tint = accent,
                modifier = Modifier.weight(1f),
            )
            CellDivider()
            SessionCell(
                icon = Icons.Filled.ArrowUpward,
                label = stringResource(R.string.session_upload),
                value = formatBytes(stats.upBytesTotal),
                tint = accent,
                modifier = Modifier.weight(1f),
            )
        }

        val exitIp = stats.exitIp
        if (exitIp != null || stats.exitCountry != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Public,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            stringResource(R.string.session_exit),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            listOfNotNull(stats.exitCountry, exitIp).joinToString("  \u2022  ")
                                .ifBlank { stringResource(R.string.session_exit_unknown) },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        if (stats.reconnects > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "${stringResource(R.string.session_reconnects)}: ${stats.reconnects}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CellDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    )
}
