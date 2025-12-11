package com.pulselink.auth

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.pulselink.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseAuthManager @Inject constructor(
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        updateAuthState(firebaseAuth.currentUser)
    }

    init {
        updateAuthState(auth.currentUser)
        auth.addAuthStateListener(authListener)
    }

    private fun updateAuthState(user: FirebaseUser?) {
        Log.d(TAG, "Auth state updated user=${user?.uid ?: "null"} anon=${user?.isAnonymous}")
        _authState.value = user?.let { AuthState.Authenticated(it) } ?: AuthState.Unauthenticated
    }

    suspend fun ensureSignedIn() {
        if (auth.currentUser != null) return
        _authState.filterIsInstance<AuthState.Authenticated>().first()
    }

    suspend fun signInSmsOnly(): Result<FirebaseUser> {
        return runCatching {
            auth.signInAnonymously().await().user ?: error("Anonymous sign-in returned no user")
        }.onFailure { error ->
            Log.w(TAG, "SMS-only sign-in failed", error)
        }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return runCatching {
            val user = auth.signInWithEmailAndPassword(email, password).await().user
                ?: error("Authentication succeeded without user payload")
            updateAuthState(user)
            Log.i(TAG, "Email/password sign-in success uid=${user.uid}")
            user
        }.onFailure { error ->
            Log.w(TAG, "Firebase email/password sign-in failed", error)
        }
    }

    suspend fun register(email: String, password: String): Result<FirebaseUser> {
        return runCatching {
            val user = auth.createUserWithEmailAndPassword(email, password).await().user
                ?: error("Account creation succeeded without user payload")
            updateAuthState(user)
            Log.i(TAG, "Account creation success uid=${user.uid}")
            user
        }.onFailure { error ->
            Log.w(TAG, "Firebase account creation failed", error)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await().user
                ?: error("Google sign-in succeeded without a Firebase user")
        }.onFailure { error ->
            Log.w(TAG, "Google sign-in failed", error)
        }
    }

    suspend fun signInWithApple(activity: ComponentActivity): Result<FirebaseUser> {
        return runCatching {
            val provider = OAuthProvider.newBuilder("apple.com").apply {
                scopes = listOf("email", "name")
            }
            val pending = auth.pendingAuthResult
            val result = if (pending != null) {
                pending.await()
            } else {
                auth.startActivityForSignInWithProvider(activity, provider.build()).await()
            }
            result.user ?: error("Apple sign-in succeeded without a Firebase user")
        }.onFailure { error ->
            Log.w(TAG, "Apple sign-in failed", error)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return runCatching {
            auth.sendPasswordResetEmail(email).await()
            Unit
        }.onFailure { error ->
            Log.w(TAG, "Password reset email failed", error)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return runCatching {
            auth.signOut()
            val googleClient = GoogleSignIn.getClient(context, googleSignInOptions())
            runCatching { googleClient.signOut().await() }
                .onFailure { error -> Log.w(TAG, "Google sign-out failed", error) }
            runCatching { googleClient.revokeAccess().await() }
                .onFailure { error -> Log.w(TAG, "Google revokeAccess failed", error) }
            Unit
        }
    }

    fun currentUser(): FirebaseUser? = auth.currentUser

    suspend fun refreshClaims() {
        runCatching { auth.currentUser?.getIdToken(true)?.await() }
            .onFailure { error -> Log.w(TAG, "Unable to refresh Firebase ID token", error) }
    }

    companion object {
        private const val TAG = "FirebaseAuthManager/Auth"
    }

    private fun googleSignInOptions(): GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
}
