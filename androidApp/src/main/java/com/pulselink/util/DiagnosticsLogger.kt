package com.pulselink.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticsLogger @Inject constructor(
    private val crashlytics: FirebaseCrashlytics
) {
    fun info(tag: String, message: String) {
        Log.i(tag, message)
        crashlytics.log("$tag: $message")
    }

    fun warn(tag: String, message: String) {
        Log.w(tag, message)
        crashlytics.log("$tag: $message")
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        crashlytics.log("$tag: $message")
        throwable?.let { crashlytics.recordException(it) }
    }
}
