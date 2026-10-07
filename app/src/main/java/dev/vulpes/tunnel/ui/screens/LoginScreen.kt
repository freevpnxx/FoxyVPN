package dev.vulpes.tunnel.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.vulpes.tunnel.R
import dev.vulpes.tunnel.data.FxaApiError
import dev.vulpes.tunnel.data.FxaAuthRepository
import kotlinx.coroutines.launch

private const val FXA_SIGNUP_URL = "https://accounts.firefox.com/signup"

/**
 * Turns whatever the auth layer threw into something a person can act on.
 *
 * Raw exception text was previously shown verbatim, which leaked internals and never matched the
 * device language. Callers pass the already-localised fallbacks in so this stays a pure mapping.
 */
private fun friendlyAuthError(
    cause: Throwable,
    signInLabel: String,
    verifyLabel: String,
    networkLabel: String,
    genericLabel: String,
): String = when (cause) {
    is FxaApiError -> when (cause.statusCode) {
        400, 401, 403 -> signInLabel
        else -> genericLabel
    }
    is java.io.IOException -> networkLabel
    else -> if (cause.message.isNullOrBlank()) genericLabel else verifyLabel
}

@Composable
fun LoginScreen(
    authRepository: FxaAuthRepository,
    onSignedIn: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var awaitingTwoFactor by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val signInLabel = stringResource(R.string.err_signin)
    val verifyLabel = stringResource(R.string.err_verify)
    val networkLabel = stringResource(R.string.err_network)
    val genericLabel = stringResource(R.string.err_generic)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.padding(top = 8.dp))
        Text(
            stringResource(R.string.login_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(top = 24.dp))

        if (!awaitingTwoFactor) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.login_email)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.padding(top = 12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.login_password)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.padding(top = 20.dp))
            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    scope.launch {
                        val result = authRepository.startLogin(email, password)
                        isLoading = false
                        result.onSuccess { needsVerification ->
                            if (needsVerification) awaitingTwoFactor = true else onSignedIn()
                        }.onFailure {
                            errorMessage = friendlyAuthError(
                                it, signInLabel, verifyLabel, networkLabel, genericLabel,
                            )
                        }
                    }
                },
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                } else {
                    Text(stringResource(R.string.action_continue))
                }
            }

            // Sign-up happens in the real browser. The app never observes those credentials;
            // the user comes back and types them here once the account is confirmed.
            Spacer(Modifier.padding(top = 8.dp))
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(FXA_SIGNUP_URL)),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.login_create_account))
            }
            Text(
                stringResource(R.string.login_create_account_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                stringResource(R.string.login_code_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.padding(top = 12.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text(stringResource(R.string.login_code)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.padding(top = 20.dp))
            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    scope.launch {
                        val result = authRepository.submitTwoFactorCode(code)
                        isLoading = false
                        result.onSuccess { onSignedIn() }
                            .onFailure {
                                errorMessage = friendlyAuthError(
                                    it, signInLabel, verifyLabel, networkLabel, genericLabel,
                                )
                            }
                    }
                },
                enabled = !isLoading && code.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                } else {
                    Text(stringResource(R.string.action_verify))
                }
            }
        }

        errorMessage?.let {
            Spacer(Modifier.padding(top = 12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
