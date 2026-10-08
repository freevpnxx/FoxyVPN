package dev.vulpes.tunnel.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vulpes.tunnel.FoxyVpnApp
import dev.vulpes.tunnel.R
import dev.vulpes.tunnel.data.model.ConnectionState
import dev.vulpes.tunnel.ui.components.AuroraBackdrop
import dev.vulpes.tunnel.ui.components.GlassCard
import dev.vulpes.tunnel.ui.components.PowerOrb
import dev.vulpes.tunnel.ui.components.QuotaCard
import dev.vulpes.tunnel.ui.components.SessionStrip
import dev.vulpes.tunnel.ui.components.StatusPill
import dev.vulpes.tunnel.ui.components.rememberTickingElapsed
import dev.vulpes.tunnel.ui.theme.LocalFoxyStatusColors
import dev.vulpes.tunnel.ui.theme.ThemeController
import dev.vulpes.tunnel.ui.theme.ThemeMode
import dev.vulpes.tunnel.vpn.FoxyVpnService
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
    val session by FoxyVpnService.sessionStats.collectAsState()
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

    val elapsedMs = rememberTickingElapsed(
        startedAtElapsedMs = session.startedAtElapsedMs,
        active = state == ConnectionState.CONNECTED,
    )

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
        ConnectionState.CONNECTED -> stringResource(R.string.status_protected)
        ConnectionState.CONNECTING -> stringResource(R.string.status_connecting)
        ConnectionState.DISCONNECTED -> stringResource(R.string.status_disconnected)
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
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
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
                                ThemeMode.SYSTEM -> stringResource(
                                    R.string.theme_system,
                                    stringResource(
                                        if (showingDark) R.string.theme_light_word else R.string.theme_dark_word,
                                    ),
                                )
                                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                ThemeMode.DARK -> stringResource(R.string.theme_dark)
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(topGap))
                Spacer(Modifier.height(8.dp))

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

                Spacer(Modifier.height(20.dp))

                AnimatedVisibility(
                    visible = entered,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                ) {
                    PowerOrb(
                        state = state,
                        accent = accent,
                        size = 184.dp,
                        connectLabel = stringResource(R.string.power_connect),
                        disconnectLabel = stringResource(R.string.power_disconnect),
                        onToggle = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (state == ConnectionState.DISCONNECTED) onRequestConnect() else onDisconnect()
                        },
                    )
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = when (state) {
                        ConnectionState.CONNECTED -> stringResource(R.string.state_connected)
                        ConnectionState.CONNECTING -> stringResource(R.string.state_connecting)
                        ConnectionState.DISCONNECTED -> stringResource(R.string.state_disconnected)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = when (state) {
                        ConnectionState.CONNECTED -> stringResource(R.string.state_connected_subtitle)
                        ConnectionState.CONNECTING -> stringResource(R.string.state_connecting_subtitle)
                        ConnectionState.DISCONNECTED -> stringResource(R.string.state_disconnected_subtitle)
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
                        SessionStrip(stats = session, elapsedMs = elapsedMs, accent = accent)
                        Spacer(Modifier.height(12.dp))
                        QuotaCard(stats = session, accent = accent)
                    }
                }

                Spacer(Modifier.height(14.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenServers,
                ) {
                    Text(
                        stringResource(R.string.home_location),
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
                                    ?: stringResource(R.string.home_location_default),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                selectedProxy?.host
                                    ?: stringResource(R.string.home_location_default_subtitle),
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

                Spacer(Modifier.height(12.dp))

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
                            stringResource(R.string.action_settings),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}
