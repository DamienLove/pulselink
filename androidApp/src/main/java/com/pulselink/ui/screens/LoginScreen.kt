package com.pulselink.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pulselink.BuildConfig
import com.pulselink.R
import com.pulselink.ui.state.LoginMode
import com.pulselink.ui.state.LoginUiState

@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleMode: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onAppleSignInClick: () -> Unit,
    onMessageConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val logoRes = if (BuildConfig.ADS_ENABLED) R.drawable.ic_logo else R.drawable.ic_pulselink_pro
    val title = if (state.mode == LoginMode.SIGN_IN) {
        stringResource(R.string.login_title_sign_in)
    } else {
        stringResource(R.string.login_title_create_account)
    }
    val subtitle = if (state.mode == LoginMode.SIGN_IN) {
        stringResource(R.string.login_subtitle_sign_in)
    } else {
        stringResource(R.string.login_subtitle_create_account)
    }
    val ctaLabel = if (state.mode == LoginMode.SIGN_IN) {
        stringResource(R.string.login_cta_sign_in)
    } else {
        stringResource(R.string.login_cta_create_account)
    }
    val toggleLabel = if (state.mode == LoginMode.SIGN_IN) {
        stringResource(R.string.login_toggle_create_account)
    } else {
        stringResource(R.string.login_toggle_sign_in)
    }

    state.userMessageRes?.let { messageRes ->
        val message = stringResource(id = messageRes)
        LaunchedEffect(messageRes) {
            snackbarHostState.showSnackbar(message)
            onMessageConsumed()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Image(
                painter = painterResource(id = logoRes),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.login_email_label)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.login_password_label)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (state.mode == LoginMode.SIGN_IN) ImeAction.Done else ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onSubmit()
                    }
                ),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            if (state.mode == LoginMode.CREATE_ACCOUNT) {
                OutlinedTextField(
                    value = state.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.login_confirm_password_label)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onSubmit()
                        }
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }
            AnimatedVisibility(visible = state.errorMessageRes != null) {
                state.errorMessageRes?.let { errorRes ->
                    Text(
                        text = stringResource(id = errorRes),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            val containerColor = if (BuildConfig.ADS_ENABLED) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondary
            }
            val contentColor = if (BuildConfig.ADS_ENABLED) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSecondary
            }

            val primaryLoading = state.isLoading || state.isSocialLoading
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSubmit()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !primaryLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                )
            ) {
                if (primaryLoading) {
                    CircularProgressIndicator(
                        color = contentColor,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = ctaLabel)
                }
            }
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    onToggleMode()
                },
                enabled = !primaryLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = if (state.mode == LoginMode.SIGN_IN) {
                        stringResource(R.string.login_large_toggle_create)
                    } else {
                        stringResource(R.string.login_large_toggle_sign_in)
                    }
                )
            }
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    onForgotPassword()
                },
                enabled = !state.isLoading && !state.isSocialLoading
            ) {
                Text(text = stringResource(R.string.login_forgot_password))
            }
            AuthDivider()
            SocialButton(
                label = stringResource(R.string.login_continue_with_google),
                onClick = {
                    focusManager.clearFocus()
                    onGoogleSignInClick()
                },
                enabled = !primaryLoading,
                showProgress = state.isSocialLoading
            )
            SocialButton(
                label = stringResource(R.string.login_continue_with_apple),
                onClick = {
                    focusManager.clearFocus()
                    onAppleSignInClick()
                },
                enabled = !primaryLoading,
                showProgress = state.isSocialLoading
            )
        }
    }
}

@Composable
private fun AuthDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Divider()
        Text(
            text = stringResource(R.string.login_continue_with_label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun SocialButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    showProgress: Boolean
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(text = label, fontWeight = FontWeight.SemiBold)
        }
    }
}
