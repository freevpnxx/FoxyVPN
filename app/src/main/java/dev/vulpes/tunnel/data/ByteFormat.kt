package dev.vulpes.tunnel.data

import java.util.Locale

private const val UNIT = 1024.0
private val UNITS = listOf("KB", "MB", "GB", "TB", "PB")

fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "\u2014"
    if (bytes < 1024) return "$bytes B"
    var value = bytes / UNIT
    var index = 0
    while (value >= UNIT && index < UNITS.lastIndex) {
        value /= UNIT
        index++
    }
    val pattern = if (value >= 100) "%.0f %s" else "%.1f %s"
    return String.format(Locale.US, pattern, value, UNITS[index])
}

fun formatBytesPerSecond(bytesPerSecond: Long): String =
    "${formatBytes(bytesPerSecond.coerceAtLeast(0))}/s"

/**
 * Renders an elapsed duration as `H:MM:SS`, or `M:SS` under an hour.
 *
 * Deliberately not locale-formatted: a stopwatch readout should not change shape between
 * locales, and Persian digits inside a monospace-ish timer jitter the layout.
 */
fun formatDuration(totalMillis: Long): String {
    val totalSeconds = (totalMillis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

/** Coarse human span for "resets in 3 days" style copy. */
fun formatRoughSpan(totalMillis: Long): String {
    val minutes = (totalMillis / 60_000L).coerceAtLeast(0L)
    return when {
        minutes < 60 -> "${minutes}m"
        minutes < 60 * 48 -> "${minutes / 60}h"
        else -> "${minutes / (60 * 24)}d"
    }
}
