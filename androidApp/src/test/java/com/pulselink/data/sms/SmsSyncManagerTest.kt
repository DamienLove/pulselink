package com.pulselink.data.sms

import com.pulselink.domain.model.PulseLinkSettings
import com.pulselink.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class SmsSyncManagerTest {

    private val smsRepository: SmsRepository = mock(SmsRepository::class.java)
    private val smsSyncTrigger: SmsSyncTrigger = mock(SmsSyncTrigger::class.java)
    private val settingsRepository: SettingsRepository = mock(SettingsRepository::class.java)

    private lateinit var syncManager: SmsSyncManager
    private val changesFlow = MutableSharedFlow<Unit>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        `when`(smsRepository.changes()).thenReturn(changesFlow)
        syncManager = SmsSyncManager(smsRepository, smsSyncTrigger, settingsRepository)
    }

    @Test
    fun `start triggers sync when enabled`() = runTest {
        val settings = mock(PulseLinkSettings::class.java)
        `when`(settings.remoteWebAccessEnabled).thenReturn(true)
        `when`(settingsRepository.settings).thenReturn(flowOf(settings))

        syncManager.start()
        changesFlow.emit(Unit)
    }
}
