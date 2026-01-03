package com.pulselink.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pulselink.R
import com.pulselink.ui.branding.brandLogoRes
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
    onSmsOnlyClick: () -> Unit,
    onGoogleClick: () -> Unit,
    onMessageConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    useProBranding: Boolean = false
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val logoRes = brandLogoRes(useProBranding)
    val baseTitle = if (state.isAnonymousUser) {
        stringResource(R.string.login_title_upgrade_account)
    } else if (state.mode == LoginMode.SIGN_IN) {
        stringResource(R.string.login_title_sign_in)
    } else {
        stringResource(R.string.login_title_create_account)
    }
    val title = if (useProBranding) "$baseTitle • Premium" else baseTitle
    val subtitle = if (state.isAnonymousUser) {
        stringResource(R.string.login_subtitle_upgrade_account)
    } else if (state.mode == LoginMode.SIGN_IN) {
        stringResource(R.string.login_subtitle_sign_in)
    } else {
        stringResource(R.string.login_subtitle_create_account)
    }
    val ctaLabel = if (state.isAnonymousUser) {
        stringResource(R.string.login_cta_link_account)
    } else if (state.mode == LoginMode.SIGN_IN) {
        stringResource(R.string.login_cta_sign_in)
    } else {
        stringResource(R.string.login_cta_create_account)
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
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = logoRes),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
            )
            if (useProBranding) {
                Text(
                    text = "Premium",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
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
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    val description = if (passwordVisible) stringResource(R.string.login_hide_password) else stringResource(R.string.login_show_password)
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                },
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
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        val description = if (confirmPasswordVisible) stringResource(R.string.login_hide_password) else stringResource(R.string.login_show_password)
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(imageVector = image, contentDescription = description)
                        }
                    },
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
            val containerColor =
                if (useProBranding) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            val contentColor =
                if (useProBranding) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary

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
            if (!state.isAnonymousUser) {
                OutlinedButton(
                    onClick = {
                        focusManager.clearFocus()
                        onToggleMode()
                    },
                    enabled = !primaryLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (state.mode == LoginMode.SIGN_IN) {
                            stringResource(R.string.login_large_toggle_create)
                        } else {
                            stringResource(R.string.login_large_toggle_sign_in)
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = stringResource(R.string.login_continue_with_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onGoogleClick()
                },
                enabled = !state.isSocialLoading && !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("google_sign_in_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                if (state.isSocialLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = stringResource(R.string.login_continue_with_google),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.login_continue_with_google),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            if (!state.isAnonymousUser) {
                Text(
                    text = stringResource(R.string.login_sms_only_helper),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        focusManager.clearFocus()
                        onSmsOnlyClick()
                    },
                    enabled = !primaryLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sms,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(text = stringResource(R.string.login_sms_only_cta))
                }
            }
        }
    }
}
