package com.pulselink

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pulselink.data.sms.PulseLinkMessage
import com.pulselink.di.LinkDebugEntryPoint
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.LinkStatus
import com.pulselink.domain.model.ManualMessageResult
import com.pulselink.domain.model.MessageDirection
import dagger.hilt.android.EntryPointAccessors
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugMessagingInstrumentation {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val entryPoint = EntryPointAccessors.fromApplication(
        context,
        LinkDebugEntryPoint::class.java
    )
    private val contactRepository = entryPoint.contactRepository()
    private val linkManager = entryPoint.linkManager()
    private val settingsRepository = entryPoint.settingsRepository()
    private val messageRepository = entryPoint.messageRepository()

    private val isFreeFlavor = BuildConfig.FLAVOR.lowercase(Locale.US).contains("free")
    private val isProFlavor = BuildConfig.FLAVOR.lowercase(Locale.US).contains("pro")

    @Test
    fun seedDebugContact() {
        runBlocking { ensureLocalContact() }
    }

    @Test
    fun sendDebugManualMessageWhenPro() {
        assumeTrue("Only the pro build should send the debug ping", isProFlavor)
        val args = InstrumentationRegistry.getArguments()
        val customBody = args.getString(ARG_BODY)
        runBlocking {
            val contact = ensureLocalContact()
            val body = customBody ?: "$DEBUG_MESSAGE_PREFIX ${System.currentTimeMillis()}"
            val result = linkManager.sendManualMessage(contact.id, body)
            assertTrue("Manual message result=$result", result is ManualMessageResult.Success)
        }
    }

    @Test
    fun verifyInboundMessageWhenFree() {
        assumeTrue("Only the free build verifies inbound pings", isFreeFlavor)
        runBlocking {
            val contact = ensureLocalContact()
            val messages = withTimeout(30_000) {
                val manualMessage = PulseLinkMessage.ManualMessage(
                    senderId = DEBUG_REMOTE_DEVICE_ID,
                    code = contact.linkCode ?: DEBUG_LINK_CODE,
                    body = "$DEBUG_MESSAGE_PREFIX ${System.currentTimeMillis()}"
                )
                linkManager.handleInbound(manualMessage, contact.phoneNumber)
                messageRepository.observeForContact(contact.id)
                    .map { items -> items.filter { it.body.startsWith(DEBUG_MESSAGE_PREFIX) } }
                    .first { it.isNotEmpty() }
            }
            val latest = messages.last()
            assertTrue(
                "Expected inbound direction for ${latest.body}",
                latest.direction == MessageDirection.INBOUND
            )
        }
    }

    private suspend fun ensureLocalContact(): Contact {
        settingsRepository.ensureDeviceId()
        val (remotePhone, displayName) = if (isProFlavor) FREE_CONTACT else PRO_CONTACT
        val existing = contactRepository.getByPhone(remotePhone)
            ?: contactRepository.observeContacts()
                .first()
                .firstOrNull { it.phoneNumber == remotePhone }
        val template = existing ?: Contact(
            displayName = displayName,
            phoneNumber = remotePhone
        )
        val updated = template.copy(
            linkStatus = LinkStatus.LINKED,
            linkCode = DEBUG_LINK_CODE,
            allowRemoteOverride = true,
            allowRemoteSoundChange = true,
            pendingApproval = false
        )
        contactRepository.upsert(updated)
        return contactRepository.getByPhone(remotePhone) ?: updated
    }

    companion object {
        private const val DEBUG_LINK_CODE = "debug-shared-link"
        private const val DEBUG_MESSAGE_PREFIX = "[DebugPing]"
        private const val DEBUG_REMOTE_DEVICE_ID = "debug-pro-device"
        private val PRO_CONTACT = "+12136760516" to "QA Pro"
        private val FREE_CONTACT = "+17209938270" to "QA Free"
        private const val ARG_BODY = "body"
    }
}
