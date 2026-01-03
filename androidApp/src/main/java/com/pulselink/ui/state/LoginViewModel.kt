package com.pulselink.ui.state

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.installations.FirebaseInstallationsException
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.pulselink.R
import com.pulselink.auth.AuthState
import com.pulselink.auth.FirebaseAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: FirebaseAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    val authState: StateFlow<AuthState> = authManager.authState

    init {
        viewModelScope.launch {
            authManager.authState.collect { authState ->
                val isAnon = (authState as? AuthState.Authenticated)?.user?.isAnonymous == true
                _uiState.update {
                    it.copy(
                        isAnonymousUser = isAnon,
                        mode = if (isAnon) LoginMode.CREATE_ACCOUNT else it.mode
                    )
                }
            }
        }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, errorMessageRes = null, userMessageRes = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessageRes = null, userMessageRes = null) }
    }

    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessageRes = null, userMessageRes = null) }
    }

    fun toggleMode() {
        _uiState.update {
            val nextMode = if (it.mode == LoginMode.SIGN_IN) LoginMode.CREATE_ACCOUNT else LoginMode.SIGN_IN
            it.copy(
                mode = nextMode,
                password = "",
                confirmPassword = "",
                errorMessageRes = null,
                userMessageRes = null
            )
        }
    }

    fun submit() {
        val state = _uiState.value
        if (state.email.isBlank()) {
            _uiState.update { it.copy(errorMessageRes = LoginError.MISSING_EMAIL.messageRes) }
            return
        }
        if (state.password.length < MIN_PASSWORD_LENGTH) {
            _uiState.update { it.copy(errorMessageRes = LoginError.SHORT_PASSWORD.messageRes) }
            return
        }
        if (state.mode == LoginMode.CREATE_ACCOUNT && state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessageRes = LoginError.PASSWORD_MISMATCH.messageRes) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessageRes = null, userMessageRes = null) }
            val sanitizedEmail = state.email.trim()
            val sanitizedPassword = state.password.trim()
            val result = withTimeoutOrNull(LOGIN_TIMEOUT_MS) {
                if (state.mode == LoginMode.CREATE_ACCOUNT && authManager.currentUser()?.isAnonymous == true) {
                    authManager.linkEmailAccount(sanitizedEmail, sanitizedPassword)
                } else if (state.mode == LoginMode.SIGN_IN) {
                    authManager.signIn(sanitizedEmail, sanitizedPassword)
                } else {
                    authManager.register(sanitizedEmail, sanitizedPassword)
                }
            } ?: Result.failure(TimeoutException("Login timed out"))
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user == null) {
                    Log.w(TAG, "Auth ${state.mode} reported success but user payload is null")
                } else {
                    Log.d(TAG, "Auth ${state.mode} success uid=${user.uid}")
                }
                delay(AUTH_SYNC_DELAY_MS)
            } else {
                result.exceptionOrNull()?.let {
                    FirebaseCrashlytics.getInstance().recordException(it)
                }
                Log.w(TAG, "Auth ${state.mode} failed", result.exceptionOrNull())
            }
            _uiState.update {
                val loginError = result.exceptionOrNull()?.toLoginError()
                it.copy(
                    isLoading = false,
                    errorMessageRes = loginError?.messageRes,
                    userMessageRes = if (result.isSuccess && state.mode == LoginMode.CREATE_ACCOUNT) {
                        LoginMessage.ACCOUNT_CREATED.messageRes
                    } else null
                )
            }
        }
    }

    fun sendPasswordReset() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessageRes = LoginError.MISSING_EMAIL.messageRes) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessageRes = null, userMessageRes = null) }
            val result = authManager.sendPasswordReset(email)
            _uiState.update {
                val loginError = result.exceptionOrNull()?.toLoginError()
                it.copy(
                    isLoading = false,
                    errorMessageRes = loginError?.messageRes,
                    userMessageRes = if (result.isSuccess) LoginMessage.RESET_SENT.messageRes else null
                )
            }
        }
    }

    fun clearTransientMessages() {
        _uiState.update { it.copy(errorMessageRes = null, userMessageRes = null) }
    }

    fun handleGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSocialLoading = true, errorMessageRes = null, userMessageRes = null) }
            val result = authManager.signInWithGoogle(idToken)
            _uiState.update {
                val loginError = result.exceptionOrNull()?.toLoginError()
                it.copy(
                    isSocialLoading = false,
                    errorMessageRes = loginError?.messageRes
                )
            }
        }
    }

    fun signInWithApple(activity: ComponentActivity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSocialLoading = true, errorMessageRes = null, userMessageRes = null) }
            val result = authManager.signInWithApple(activity)
            _uiState.update {
                val loginError = result.exceptionOrNull()?.toLoginError()
                it.copy(
                    isSocialLoading = false,
                    errorMessageRes = loginError?.messageRes
                )
            }
        }
    }

    fun reportExternalError() {
        _uiState.update {
            it.copy(
                isSocialLoading = false,
                errorMessageRes = LoginError.GENERIC.messageRes
            )
        }
    }

    fun signInSmsOnly() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessageRes = null, userMessageRes = null) }
            val result = authManager.signInSmsOnly()
            _uiState.update {
                val loginError = result.exceptionOrNull()?.toLoginError()
                it.copy(
                    isLoading = false,
                    errorMessageRes = loginError?.messageRes
                )
            }
        }
    }

    private fun Throwable.toLoginError(): LoginError {
        when (this) {
            is TimeoutException -> return LoginError.NETWORK_BLOCKED
            is IOException -> return LoginError.NETWORK
            is FirebaseNetworkException -> return LoginError.NETWORK
            is FirebaseInstallationsException -> return LoginError.NETWORK_BLOCKED
        }
        val normalized = message.orEmpty()
        return when {
            normalized.contains("installations", ignoreCase = true) -> LoginError.NETWORK_BLOCKED
            normalized.contains("recaptcha", ignoreCase = true) -> LoginError.NETWORK_BLOCKED
            normalized.contains("network", ignoreCase = true) -> LoginError.NETWORK
            normalized.contains("password", ignoreCase = true) -> LoginError.INVALID_CREDENTIALS
            normalized.contains("no user", ignoreCase = true) -> LoginError.INVALID_CREDENTIALS
            normalized.contains("already in use", ignoreCase = true) -> LoginError.EMAIL_IN_USE
            else -> LoginError.GENERIC
        }
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private const val LOGIN_TIMEOUT_MS = 15_000L
        private const val AUTH_SYNC_DELAY_MS = 150L
        private const val TAG = "LoginViewModel/Auth"
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val mode: LoginMode = LoginMode.SIGN_IN,
    val isLoading: Boolean = false,
    val isSocialLoading: Boolean = false,
    val isAnonymousUser: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
    @StringRes val userMessageRes: Int? = null
)

enum class LoginMode { SIGN_IN, CREATE_ACCOUNT }

private enum class LoginError(@StringRes val messageRes: Int) {
    MISSING_EMAIL(R.string.login_error_missing_email),
    SHORT_PASSWORD(R.string.login_error_short_password),
    PASSWORD_MISMATCH(R.string.login_error_password_mismatch),
    INVALID_CREDENTIALS(R.string.login_error_invalid_credentials),
    NETWORK(R.string.login_error_network),
    NETWORK_BLOCKED(R.string.login_error_network_blocked),
    EMAIL_IN_USE(R.string.login_error_email_in_use),
    GENERIC(R.string.login_error_generic)
}

private enum class LoginMessage(@StringRes val messageRes: Int) {
    RESET_SENT(R.string.login_message_reset_sent),
    ACCOUNT_CREATED(R.string.login_message_account_created)
}
