package com.pulselink.util

import java.net.URLEncoder

object EmailUtils {
    /**
     * Constructs a mailto URI string with properly encoded subject and body.
     * Uses %20 for spaces instead of + to ensure compatibility with all email clients.
     */
    fun createMailtoUriString(email: String?, subject: String, body: String): String {
        val encodedSubject = URLEncoder.encode(subject, "UTF-8").replace("+", "%20")
        val encodedBody = URLEncoder.encode(body, "UTF-8").replace("+", "%20")
        val emailPart = email ?: ""
        return "mailto:$emailPart?subject=$encodedSubject&body=$encodedBody"
    }
}
