package dev.vulpes.tunnel.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.vulpes.tunnel.R
import dev.vulpes.tunnel.data.FxaAuthRepository
import dev.vulpes.tunnel.data.GUARDIAN_ENDPOINT_DEFAULT
import dev.vulpes.tunnel.data.GuardianClient
import dev.vulpes.tunnel.data.formatBytes
import dev.vulpes.tunnel.data.model.Entitlement
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    authRepository: FxaAuthRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loadFailedLabel = stringResource(R.string.account_load_failed)
    val signedOutLabel = stringResource(R.string.err_not_signed_in)
    var entitlement by remember { mutableStateOf<Entitlement?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        isLoading = true
        errorMessage = null
        scope.launch {
            val accessToken = authRepository.currentAccessToken()
            if (accessToken == null) {
                isLoading = false
                errorMessage = signedOutLabel
                return@launch
            }
            runCatching { GuardianClient().fetchUserInfo(GUARDIAN_ENDPOINT_DEFAULT, accessToken) }
                .onSuccess { entitlement = it }
                .onFailure { errorMessage = loadFailedLabel }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.account_refresh))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.padding(start = 12.dp))
                Column {
                    Text(stringResource(R.string.account_provider), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.account_signed_in),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.padding(top = 20.dp))

            when {
                isLoading -> Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                errorMessage != null -> Column {
                    Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.padding(top = 8.dp))
                    TextButton(onClick = { refresh() }) { Text(stringResource(R.string.action_retry)) }
                }
                entitlement != null -> {
                    val info = entitlement!!
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            AccountInfoRow(
                                stringResource(R.string.account_subscription),
                                if (info.subscribed) {
                                    stringResource(R.string.account_active)
                                } else {
                                    stringResource(R.string.account_inactive)
                                },
                            )
                            AccountInfoRow(stringResource(R.string.account_id), info.uid.ifBlank { "\u2014" })

                            AccountInfoRow(
                                stringResource(R.string.account_data_remaining),
                                when {
                                    !info.limitedBandwidth -> stringResource(R.string.account_unlimited)
                                    info.quotaRemaining != null -> formatBytes(info.quotaRemaining)
                                    info.maxBytes != null -> stringResource(
                                        R.string.account_limited_to,
                                        formatBytes(info.maxBytes),
                                    )
                                    else -> stringResource(R.string.account_limited)
                                },
                            )
                        }
                    }
                    Spacer(Modifier.padding(top = 16.dp))
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GUARDIAN_ENDPOINT_DEFAULT)))
                    }) {
                        Text(stringResource(R.string.account_manage))
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountInfoRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
