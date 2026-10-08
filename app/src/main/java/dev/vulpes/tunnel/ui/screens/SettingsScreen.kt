package dev.vulpes.tunnel.ui.screens

import dev.vulpes.tunnel.data.LocaleManager
import android.app.Activity
import android.content.Context
import android.provider.Settings
import android.os.PowerManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.vulpes.tunnel.BuildConfig
import dev.vulpes.tunnel.R
import dev.vulpes.tunnel.data.SettingsStore

private const val GITHUB_URL = "https://github.com/freevpnxx/FoxyVPN"

@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    onOpenLogs: () -> Unit,
    onOpenAccount: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val notSetLabel = stringResource(R.string.value_not_set)
    val noneLabel = stringResource(R.string.value_none)
    var exitCheckEnabled by remember { mutableStateOf(settingsStore.exitCheckEnabled) }
    var killSwitchEnabled by remember { mutableStateOf(settingsStore.killSwitchEnabled) }
    var appLanguage by remember { mutableStateOf(settingsStore.appLanguage) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val batteryUnrestricted = remember {
        (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)
            ?.isIgnoringBatteryOptimizations(context.packageName) == true
    }
    var dohProvider by remember { mutableStateOf(settingsStore.dohProvider) }
    val dohLabel = dohProvider.labelRes?.let { stringResource(it) } ?: dohProvider.label
    var customDnsEnabled by remember { mutableStateOf(settingsStore.customDnsEnabled) }
    var customDnsServer by remember { mutableStateOf(settingsStore.customDnsServer) }
    var socksBindAddress by remember { mutableStateOf(settingsStore.socksBindAddress) }
    var socksPort by remember { mutableStateOf(settingsStore.socksPort) }
    var proxyOnlyMode by remember { mutableStateOf(settingsStore.proxyOnlyMode) }
    var customEdgeAddress by remember { mutableStateOf(settingsStore.customEdgeAddress) }
    var upstreamProxyEnabled by remember { mutableStateOf(settingsStore.upstreamProxyEnabled) }
    var upstreamProxyType by remember { mutableStateOf(settingsStore.upstreamProxyType) }
    var upstreamProxyHost by remember { mutableStateOf(settingsStore.upstreamProxyHost) }
    var upstreamProxyPort by remember { mutableStateOf(settingsStore.upstreamProxyPort) }
    var upstreamProxyUsername by remember { mutableStateOf(settingsStore.upstreamProxyUsername) }
    var upstreamProxyPassword by remember { mutableStateOf(settingsStore.upstreamProxyPassword) }
    var excludedApps by remember { mutableStateOf(settingsStore.excludedApps) }
    var showDohProviderDialog by remember { mutableStateOf(false) }
    var showCustomDnsDialog by remember { mutableStateOf(false) }
    var showSocksBindDialog by remember { mutableStateOf(false) }
    var showSocksPortDialog by remember { mutableStateOf(false) }
    var showEdgeAddressDialog by remember { mutableStateOf(false) }
    var showUpstreamProxyTypeDialog by remember { mutableStateOf(false) }
    var showUpstreamProxyAddressDialog by remember { mutableStateOf(false) }
    var showUpstreamProxyCredentialsDialog by remember { mutableStateOf(false) }
    var showSplitTunnelDialog by remember { mutableStateOf(false) }

    if (showLanguageDialog) {
        LanguagePickerDialog(
            current = appLanguage,
            onDismiss = { showLanguageDialog = false },
            onConfirm = { picked ->
                showLanguageDialog = false
                if (picked != appLanguage) {
                    appLanguage = picked
                    settingsStore.appLanguage = picked
                    // The whole tree has to be rebuilt so every string re-resolves, and the
                    // layout direction flips when switching to Persian.
                    (context as? Activity)?.recreate()
                }
            },
        )
    }
    if (showDohProviderDialog) {
        DohProviderPickerDialog(
            current = dohProvider,
            onDismiss = { showDohProviderDialog = false },
            onConfirm = {
                dohProvider = it
                settingsStore.dohProvider = it
                showDohProviderDialog = false
            },
        )
    }
    if (showCustomDnsDialog) {
        CustomDnsPickerDialog(
            current = customDnsServer,
            onDismiss = { showCustomDnsDialog = false },
            onConfirm = {
                customDnsServer = it
                settingsStore.customDnsServer = it
                showCustomDnsDialog = false
            },
        )
    }
    if (showSocksBindDialog) {
        SocksBindAddressPickerDialog(
            current = socksBindAddress,
            onDismiss = { showSocksBindDialog = false },
            onConfirm = {
                socksBindAddress = it
                settingsStore.socksBindAddress = it
                showSocksBindDialog = false
            },
        )
    }
    if (showSocksPortDialog) {
        SocksPortPickerDialog(
            current = socksPort,
            onDismiss = { showSocksPortDialog = false },
            onConfirm = {
                socksPort = it
                settingsStore.socksPort = it
                showSocksPortDialog = false
            },
        )
    }
    if (showEdgeAddressDialog) {
        CustomEdgeAddressDialog(
            current = customEdgeAddress,
            onDismiss = { showEdgeAddressDialog = false },
            onConfirm = {
                customEdgeAddress = it
                settingsStore.customEdgeAddress = it
                showEdgeAddressDialog = false
            },
        )
    }
    if (showUpstreamProxyTypeDialog) {
        UpstreamProxyTypePickerDialog(
            current = upstreamProxyType,
            onDismiss = { showUpstreamProxyTypeDialog = false },
            onConfirm = {
                upstreamProxyType = it
                settingsStore.upstreamProxyType = it
                showUpstreamProxyTypeDialog = false
            },
        )
    }
    if (showUpstreamProxyAddressDialog) {
        UpstreamProxyAddressDialog(
            currentHost = upstreamProxyHost,
            currentPort = upstreamProxyPort,
            onDismiss = { showUpstreamProxyAddressDialog = false },
            onConfirm = { host, port ->
                upstreamProxyHost = host
                upstreamProxyPort = port
                settingsStore.upstreamProxyHost = host
                settingsStore.upstreamProxyPort = port
                showUpstreamProxyAddressDialog = false
            },
        )
    }
    if (showUpstreamProxyCredentialsDialog) {
        UpstreamProxyCredentialsDialog(
            currentUsername = upstreamProxyUsername,
            currentPassword = upstreamProxyPassword,
            onDismiss = { showUpstreamProxyCredentialsDialog = false },
            onConfirm = { username, password ->
                upstreamProxyUsername = username
                upstreamProxyPassword = password
                settingsStore.upstreamProxyUsername = username
                settingsStore.upstreamProxyPassword = password
                showUpstreamProxyCredentialsDialog = false
            },
        )
    }
    if (showSplitTunnelDialog) {
        SplitTunnelDialog(
            current = excludedApps,
            onDismiss = { showSplitTunnelDialog = false },
            onConfirm = {
                excludedApps = it
                settingsStore.excludedApps = it
                showSplitTunnelDialog = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionLabel(stringResource(R.string.settings_section_appearance))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_language)) },
                supportingContent = { Text(languageLabel(appLanguage)) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable { showLanguageDialog = true },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionLabel(stringResource(R.string.settings_section_connection))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_verify_exit)) },
                supportingContent = { Text(stringResource(R.string.settings_verify_exit_desc)) },
                trailingContent = {
                    Switch(
                        checked = exitCheckEnabled,
                        onCheckedChange = {
                            exitCheckEnabled = it
                            settingsStore.exitCheckEnabled = it
                        },
                    )
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.killswitch_title)) },
                supportingContent = { Text(stringResource(R.string.killswitch_subtitle)) },
                trailingContent = {
                    Switch(
                        checked = killSwitchEnabled,
                        onCheckedChange = {
                            killSwitchEnabled = it
                            settingsStore.killSwitchEnabled = it
                        },
                    )
                },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionLabel(stringResource(R.string.settings_section_reliability))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_always_on)) },
                supportingContent = { Text(stringResource(R.string.settings_always_on_desc)) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
                    }
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_battery)) },
                supportingContent = {
                    Text(
                        if (batteryUnrestricted) stringResource(R.string.settings_battery_ok)
                        else stringResource(R.string.settings_battery_desc),
                    )
                },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                        )
                    }
                },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionLabel(stringResource(R.string.settings_section_dns))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_encrypted_dns)) },
                supportingContent = { Text(dohLabel) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable { showDohProviderDialog = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_custom_dns)) },
                supportingContent = { Text(stringResource(R.string.settings_custom_dns_desc)) },
                trailingContent = {
                    Switch(
                        checked = customDnsEnabled,
                        onCheckedChange = {
                            customDnsEnabled = it
                            settingsStore.customDnsEnabled = it
                        },
                    )
                },
            )
            if (customDnsEnabled) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_dns_server)) },
                    supportingContent = {
                        val presetLabel = SettingsStore.CUSTOM_DNS_PRESETS
                            .firstOrNull { it.first == customDnsServer }
                            ?.second
                        Text(presetLabel ?: customDnsServer)
                    },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { showCustomDnsDialog = true },
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionLabel(stringResource(R.string.settings_section_local_proxy))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_proxy_only)) },
                supportingContent = { Text(stringResource(R.string.settings_proxy_only_desc)) },
                trailingContent = {
                    Switch(
                        checked = proxyOnlyMode,
                        onCheckedChange = {
                            proxyOnlyMode = it
                            settingsStore.proxyOnlyMode = it
                        },
                    )
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_local_address)) },
                supportingContent = { Text(socksBindAddress) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable { showSocksBindDialog = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_local_port)) },
                supportingContent = { Text(socksPort.toString()) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable { showSocksPortDialog = true },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionLabel(stringResource(R.string.settings_section_split))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_excluded_apps)) },
                supportingContent = {
                    Text(
                        if (excludedApps.isEmpty()) {
                            stringResource(R.string.value_none)
                        } else {
                            stringResource(R.string.settings_excluded_count, excludedApps.size)
                        },
                    )
                },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable { showSplitTunnelDialog = true },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionLabel(stringResource(R.string.settings_section_advanced))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_custom_edge)) },
                supportingContent = { Text(customEdgeAddress.ifBlank { notSetLabel }) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable { showEdgeAddressDialog = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_upstream)) },
                supportingContent = { Text(stringResource(R.string.settings_upstream_desc)) },
                trailingContent = {
                    Switch(
                        checked = upstreamProxyEnabled,
                        onCheckedChange = {
                            upstreamProxyEnabled = it
                            settingsStore.upstreamProxyEnabled = it
                        },
                    )
                },
            )
            if (upstreamProxyEnabled) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_proxy_type)) },
                    supportingContent = { Text(upstreamProxyType.name) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { showUpstreamProxyTypeDialog = true },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_proxy_address)) },
                    supportingContent = {
                        Text(
                            if (upstreamProxyHost.isBlank()) {
                                notSetLabel
                            } else {
                                "$upstreamProxyHost:$upstreamProxyPort"
                            },
                        )
                    },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { showUpstreamProxyAddressDialog = true },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_proxy_credentials)) },
                    supportingContent = {
                        Text(upstreamProxyUsername.ifBlank { noneLabel })
                    },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { showUpstreamProxyCredentialsDialog = true },
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionLabel(stringResource(R.string.settings_section_about))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_view_logs)) },
                supportingContent = { Text(stringResource(R.string.settings_view_logs_desc)) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenLogs),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version)) },
                supportingContent = { Text(BuildConfig.VERSION_NAME) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_source)) },
                supportingContent = { Text(GITHUB_URL) },
                leadingContent = { Icon(Icons.Filled.Code, contentDescription = null) },
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)))
                },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionLabel(stringResource(R.string.settings_section_account))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_manage_account)) },
                supportingContent = { Text(stringResource(R.string.settings_account_desc)) },
                leadingContent = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenAccount),
            )
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Text("  " + stringResource(R.string.settings_sign_out))
            }
        }
    }
}

@Composable
private fun DohProviderPickerDialog(
    current: SettingsStore.DohProvider,
    onDismiss: () -> Unit,
    onConfirm: (SettingsStore.DohProvider) -> Unit,
) {
    var selected by remember { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_encrypted_dns)) },
        text = {
            Column {
                SettingsStore.DohProvider.entries.forEach { provider ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected == provider, onClick = { selected = provider })
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(selected = selected == provider, onClick = { selected = provider })
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(provider.label)
                            val detail = when (provider) {
                                SettingsStore.DohProvider.AUTOMATIC -> stringResource(R.string.settings_dns_automatic)
                                SettingsStore.DohProvider.OFF -> stringResource(R.string.settings_dns_off)
                                else -> provider.addresses.joinToString(", ")
                            }
                            Text(
                                detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun CustomDnsPickerDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(current) }
    val isValid = SettingsStore.isValidDnsServer(value)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_dns_server)) },
        text = {
            Column {
                SettingsStore.CUSTOM_DNS_PRESETS.forEach { (address, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = value == address, onClick = { value = address })
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(selected = value == address, onClick = { value = address })
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.trim() },
                    label = { Text(stringResource(R.string.settings_edge_host_hint)) },
                    singleLine = true,
                    isError = value.isNotBlank() && !isValid,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }, enabled = isValid) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun CustomEdgeAddressDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(current) }
    val trimmed = value.trim()
    val isValid = trimmed.isEmpty() || SettingsStore.isValidEdgeHost(trimmed)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_custom_edge)) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.settings_edge_host_hint)) },
                    singleLine = true,
                    isError = !isValid,
                )
                Text(
                    stringResource(R.string.settings_edge_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(trimmed) }, enabled = isValid) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun SocksBindAddressPickerDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var selected by remember { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_local_address)) },
        text = {
            Column {
                SettingsStore.SOCKS_BIND_ADDRESS_PRESETS.forEach { (address, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected == address, onClick = { selected = address })
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(selected = selected == address, onClick = { selected = address })
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun SocksPortPickerDialog(
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var portText by remember { mutableStateOf(current.toString()) }
    val parsedPort = portText.toIntOrNull()
    val isValid = parsedPort != null && parsedPort in 1..65_535

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_local_port)) },
        text = {
            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                label = { Text(stringResource(R.string.settings_hint_port)) },
                singleLine = true,
                isError = portText.isNotBlank() && !isValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(onClick = { parsedPort?.let { onConfirm(it) } }, enabled = isValid) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun UpstreamProxyTypePickerDialog(
    current: SettingsStore.UpstreamProxyType,
    onDismiss: () -> Unit,
    onConfirm: (SettingsStore.UpstreamProxyType) -> Unit,
) {
    var selected by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_proxy_type)) },
        text = {
            Column {
                SettingsStore.UpstreamProxyType.entries.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected == type, onClick = { selected = type })
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(selected = selected == type, onClick = { selected = type })
                        Text(type.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun UpstreamProxyAddressDialog(
    currentHost: String,
    currentPort: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit,
) {
    var host by remember { mutableStateOf(currentHost) }
    var portText by remember { mutableStateOf(currentPort.toString()) }
    val parsedPort = portText.toIntOrNull()
    val isValid = host.isNotBlank() && parsedPort != null && parsedPort in 1..65_535

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_proxy_address)) },
        text = {
            Column {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it.trim() },
                    label = { Text(stringResource(R.string.settings_edge_host_hint)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                    label = { Text(stringResource(R.string.settings_hint_port)) },
                    singleLine = true,
                    isError = portText.isNotBlank() && (parsedPort == null || parsedPort !in 1..65_535),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { parsedPort?.let { onConfirm(host, it) } }, enabled = isValid) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun UpstreamProxyCredentialsDialog(
    currentUsername: String,
    currentPassword: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var username by remember { mutableStateOf(currentUsername) }
    var password by remember { mutableStateOf(currentPassword) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_proxy_credentials)) },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.settings_hint_username)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.settings_hint_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(username, password) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private data class InstalledAppEntry(val packageName: String, val label: String)

@Composable
private fun SplitTunnelDialog(
    current: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(current) }
    var searchQuery by remember { mutableStateOf("") }

    val installedApps by androidx.compose.runtime.produceState<List<InstalledAppEntry>>(initialValue = emptyList()) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val packageManager = context.packageManager
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .asSequence()
                .filter { it.packageName != context.packageName }
                .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
                .map { info: android.content.pm.ApplicationInfo ->
                    InstalledAppEntry(info.packageName, info.loadLabel(packageManager).toString())
                }
                .sortedBy { it.label.lowercase() }
                .toList()
        }
    }
    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_section_split)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.settings_split_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.settings_search_apps)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
                if (installedApps.isEmpty()) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally)
                    )
                } else if (filteredApps.isEmpty()) {
                    Text(
                        stringResource(R.string.settings_no_apps_match, searchQuery),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(360.dp)) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val isChecked = selected.contains(app.packageName)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected = if (isChecked) {
                                            selected - app.packageName
                                        } else {
                                            selected + app.packageName
                                        }
                                    }
                                    .padding(vertical = 2.dp),
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        selected = if (it) selected + app.packageName else selected - app.packageName
                                    },
                                )
                                Text(app.label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun languageLabel(tag: String): String = when (tag) {
    LocaleManager.ENGLISH -> stringResource(R.string.lang_en)
    LocaleManager.PERSIAN -> stringResource(R.string.lang_fa)
    else -> stringResource(R.string.lang_system)
}

@Composable
private fun LanguagePickerDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val options = listOf(
        LocaleManager.SYSTEM to stringResource(R.string.lang_system),
        LocaleManager.ENGLISH to stringResource(R.string.lang_en),
        LocaleManager.PERSIAN to stringResource(R.string.lang_fa),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            Column {
                options.forEach { (tag, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(tag) }
                            .padding(vertical = 10.dp),
                    ) {
                        RadioButton(selected = tag == current, onClick = { onConfirm(tag) })
                        Text(label, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
