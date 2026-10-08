package dev.vulpes.tunnel.ui.screens

import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.border
import java.util.Locale
import androidx.compose.ui.platform.LocalConfiguration
import dev.vulpes.tunnel.vpn.PingUtil
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import dev.vulpes.tunnel.ui.components.QuotaSection
import dev.vulpes.tunnel.ui.components.SessionSection
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
    onOpenLogs: () -> Unit,
) {
    val state by FoxyVpnService.state.collectAsState()
    val lastError by FoxyVpnService.lastError.collectAsState()
    val session by FoxyVpnService.sessionStats.collectAsState()
    val selectedProxy by app.proxyStateStore.selectedProxyFlow.collectAsState()

    // Latency of the server currently selected, so the home screen can show it next to the flag.
    val uiLocale = LocalConfiguration.current.locales[0]
    var serverPingMs by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(selectedProxy?.host, selectedProxy?.port) {
        serverPingMs = null
        val proxy = selectedProxy ?: return@LaunchedEffect
        serverPingMs = PingUtil.sample(proxy.host, proxy.port)?.latencyMs
    }

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
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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

                val error = lastError
                if (error != null && state != ConnectionState.CONNECTING) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(26.dp))

                // One dashboard card instead of three stacked ones: the server row on top, then
                // the session metrics and the remaining allowance behind dividers.
                AnimatedVisibility(
                    visible = entered,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onOpenServers)
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val flag = selectedProxy?.let { flagFor(it.countryCode) }.orEmpty()
                            if (flag.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(accent.copy(alpha = 0.10f))
                                        .border(
                                            1.dp,
                                            accent.copy(alpha = 0.22f),
                                            RoundedCornerShape(15.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(flag, fontSize = 25.sp)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(accent.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Public,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                // The server list reports English country names; render them in
                                // the app's own language so the card actually translates.
                                val countryLabel = selectedProxy?.let { proxy ->
                                    val code = proxy.countryCode.trim().uppercase()
                                    if (code.length == 2) {
                                        Locale("", code).getDisplayCountry(uiLocale)
                                            .ifBlank { proxy.countryName.ifBlank { code } }
                                    } else {
                                        proxy.countryName.ifBlank { proxy.countryCode }
                                    }
                                } ?: stringResource(R.string.home_servers_default)
                                Text(
                                    countryLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    selectedProxy?.let { proxy ->
                                        stringResource(R.string.home_server_host, proxy.host)
                                    } ?: stringResource(R.string.home_servers_default_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            val ping = serverPingMs
                            if (ping != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(accent.copy(alpha = 0.14f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    Text(
                                        stringResource(R.string.locations_latency_value, ping),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = accent,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (state == ConnectionState.CONNECTED) {
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                            Spacer(Modifier.height(16.dp))
                            SessionSection(stats = session, elapsedMs = elapsedMs, accent = accent)
                            Spacer(Modifier.height(18.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                            Spacer(Modifier.height(16.dp))
                            QuotaSection(stats = session, accent = accent)
                        }
                    }
                }

                // Keep the last card clear of the corner button.
                Spacer(Modifier.height(96.dp))
            }

            // Corner-anchored. BottomEnd follows the layout direction, so this lands bottom-right
            // in English and bottom-left in Persian with no special casing.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        CircleShape,
                    )
                    .clickable(onClick = onOpenLogs),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Code,
                    contentDescription = stringResource(R.string.settings_view_logs),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
