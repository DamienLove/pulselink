package com.pulselink.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseAuthManager @Inject constructor(
    private val auth: FirebaseAuth
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var signingIn = false

    fun ensureSignedIn() {
        if (auth.currentUser != null || signingIn) return
        signingIn = true
        scope.launch {
            runCatching { auth.signInAnonymously().await() }
                .onFailure { error -> Log.w(TAG, "Firebase anonymous sign-in failed", error) }
            signingIn = false
        }
    }

    suspend fun currentUser(): FirebaseUser? {
        auth.currentUser?.let { return it }
        return runCatching { auth.signInAnonymously().await()?.user }
            .onFailure { error -> Log.w(TAG, "Firebase sign-in attempt failed", error) }
            .getOrNull()
    }

    suspend fun refreshClaims() {
        runCatching { auth.currentUser?.getIdToken(true)?.await() }
            .onFailure { error -> Log.w(TAG, "Unable to refresh Firebase ID token", error) }
    }

    companion object {
        private const val TAG = "FirebaseAuthManager"
    }
}
