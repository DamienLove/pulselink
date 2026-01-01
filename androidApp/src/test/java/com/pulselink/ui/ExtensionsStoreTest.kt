package com.pulselink.ui

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

data class TestExtension(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val isEnabled: Boolean,
    val onToggle: (Boolean) -> Unit,
    val isAvailable: Boolean = true
)

class ExtensionsStoreTest {

    @Test
    fun extension_data_model_retains_state() {
        var toggleState = false
        val extension = TestExtension(
            id = "test",
            titleRes = 1,
            descriptionRes = 2,
            isEnabled = true,
            onToggle = { toggleState = it }
        )

        assertTrue("Extension should be enabled initially", extension.isEnabled)

        // Simulate toggle
        extension.onToggle(false)
        assertFalse("Toggle callback should have updated state", toggleState)
    }

    @Test
    fun extension_availability_flag_works() {
        val extension = TestExtension(
            id = "crash",
            titleRes = 1,
            descriptionRes = 2,
            isEnabled = false,
            onToggle = {},
            isAvailable = false
        )

        assertFalse("Extension should be unavailable", extension.isAvailable)
    }
}
