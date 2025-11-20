package com.pulselink.auth

import com.google.firebase.auth.FirebaseUser

sealed interface AuthState {
    object Loading : AuthState
    object Unauthenticated : AuthState
    data class Authenticated(val user: FirebaseUser) : AuthState
}
