package com.vauth.foxyvpn.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.FoxyVpnApp
import com.vauth.foxyvpn.data.formatBytesPerSecond
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.ui.components.AuroraBackdrop
import com.vauth.foxyvpn.ui.components.GlassCard
import com.vauth.foxyvpn.ui.components.PowerOrb
import com.vauth.foxyvpn.ui.components.StatBlock
import com.vauth.foxyvpn.ui.components.StatusPill
import com.vauth.foxyvpn.ui.theme.LocalFoxyStatusColors
import com.vauth.foxyvpn.ui.theme.ThemeController
import com.vauth.foxyvpn.ui.theme.ThemeMode
import com.vauth.foxyvpn.vpn.FoxyVpnService
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    app: FoxyVpnApp,
    themeController: ThemeController,
    onRequestConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenServers: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by FoxyVpnService.state.collectAsState()
    val lastError by FoxyVpnService.lastError.collectAsState()
    val speed by FoxyVpnService.speed.collectAsState()
    val selectedProxy by app.proxyStateStore.selectedProxyFlow.collectAsState()

    val statusColors = LocalFoxyStatusColors.current
    val systemInDarkTheme = isSystemInDarkTheme()
    val haptics = LocalHapticFeedback.current

    val targetAccent = when (state) {
        ConnectionState.CONNECTED -> statusColors.connected
        ConnectionState.CONNECTING -> statusColors.connecting
        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.primary
    }
    val accent by animateColorAsState(targetValue = targetAccent, label = "home-accent")

    // Stagger the entrance so the screen assembles itself rather than popping in as one block.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(80)
        entered = true
    }
    val enterSpec = remember {
        spring<Dp>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    }
    val topGap by animateDpAsState(
        targetValue = if (entered) 0.dp else 28.dp,
        animationSpec = enterSpec,
        label = "home-entrance-gap",
    )

    val statusLabel = when (state) {
        ConnectionState.CONNECTED -> "Protected"
        ConnectionState.CONNECTING -> "Establishing tunnel"
        ConnectionState.DISCONNECTED -> "Not connected"
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("FoxyVPN", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    val mode = themeController.mode
                    val showingDark = themeController.resolveDark(systemInDarkTheme)
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(42.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                role = Role.Button,
                                onClick = { themeController.toggle(systemInDarkTheme) },
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    themeController.set(ThemeMode.SYSTEM)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = when (mode) {
                                ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
                                ThemeMode.LIGHT -> Icons.Filled.LightMode
                                ThemeMode.DARK -> Icons.Filled.DarkMode
                            },
                            contentDescription = when (mode) {
                                ThemeMode.SYSTEM ->
                                    "Theme: follow system. Tap to switch to ${if (showingDark) "light" else "dark"} mode"
                                ThemeMode.LIGHT -> "Theme: light. Tap for dark mode, long press to follow the system"
                                ThemeMode.DARK -> "Theme: dark. Tap for light mode, long press to follow the system"
                            },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            AuroraBackdrop(
                accent = MaterialTheme.colorScheme.primary,
                support = MaterialTheme.colorScheme.tertiary,
                tertiary = MaterialTheme.colorScheme.secondary,
                intensity = if (systemInDarkTheme) 0.85f else 0.40f,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(Modifier.height(topGap))

                AnimatedVisibility(
                    visible = entered,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                ) {
                    StatusPill(
                        text = statusLabel,
                        dotColor = accent,
                        pulsing = state != ConnectionState.DISCONNECTED,
                    )
                }

                Spacer(Modifier.height(22.dp))

                AnimatedVisibility(
                    visible = entered,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                ) {
                    PowerOrb(
                        state = state,
                        accent = accent,
                        onToggle = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (state == ConnectionState.DISCONNECTED) onRequestConnect() else onDisconnect()
                        },
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = when (state) {
                        ConnectionState.CONNECTED -> "Connected"
                        ConnectionState.CONNECTING -> "Connecting\u2026"
                        ConnectionState.DISCONNECTED -> "Disconnected"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = when (state) {
                        ConnectionState.CONNECTED -> "All traffic is routed through the tunnel"
                        ConnectionState.CONNECTING -> "Tap to cancel"
                        ConnectionState.DISCONNECTED -> "Tap the ring to start the tunnel"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val error = lastError
                if (error != null && state != ConnectionState.CONNECTING) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                AnimatedVisibility(
                    visible = state == ConnectionState.CONNECTED,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                ) {
                    Column {
                        Spacer(Modifier.height(18.dp))
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                StatBlock(
                                    label = "Download",
                                    value = formatBytesPerSecond(speed.downBytesPerSecond),
                                    accent = statusColors.connected,
                                    modifier = Modifier.weight(1f),
                                )
                                Box(
                                    Modifier
                                        .width(1.dp)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        ),
                                )
                                StatBlock(
                                    label = "Upload",
                                    value = formatBytesPerSecond(speed.upBytesPerSecond),
                                    accent = statusColors.connected,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenServers,
                ) {
                    Text(
                        "Location",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Public,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                selectedProxy?.let { it.countryName.ifBlank { it.countryCode } }
                                    ?: "Recommended",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                selectedProxy?.let { it.host } ?: "Fastest available server",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable(onClick = onOpenSettings)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Settings",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
}
