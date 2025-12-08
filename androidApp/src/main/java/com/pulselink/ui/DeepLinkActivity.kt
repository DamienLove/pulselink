package com.pulselink.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Lightweight trampoline to bring the user into MainActivity from app/website links.
 * We keep logic minimal so verification succeeds even before app UI initializes.
 */
class DeepLinkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            intent?.data?.let { data ->
                putExtra(EXTRA_DEEP_LINK_URI, data.toString())
            }
        }
        startActivity(target)
        finish()
    }

    companion object {
        const val EXTRA_DEEP_LINK_URI = "deep_link_uri"
    }
}
