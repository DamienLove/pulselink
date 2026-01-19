package com.pulselink

import com.pulselink.util.EmailUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class EmailEncodingTest {

    @Test
    fun testEmailLinkEncoding() {
        val rawSubject = "Override Instructions"
        val rawBody = "You have been set as a trusted contact. Even without the PulseLink app, you can trigger an emergency alert on my phone by texting exactly:\n\n'pulselink 1234 emergency'\n\nto my number."
        val contactEmail = "test@example.com"

        // Use the actual utility function
        val uriString = EmailUtils.createMailtoUriString(contactEmail, rawSubject, rawBody)

        val expectedSubject = "Override%20Instructions"

        // Verify the structure
        assert(uriString.startsWith("mailto:test@example.com?subject=Override%20Instructions&body="))

        // Verify encoding of spaces in body
        assert(uriString.contains("You%20have%20been%20set"))
        // Verify + is NOT used for spaces
        assert(!uriString.contains("+"))
    }
}
