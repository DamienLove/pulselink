package com.pulselink.di

import com.pulselink.data.link.ContactLinkManager
import com.pulselink.domain.repository.ContactRepository
import com.pulselink.domain.repository.MessageRepository
import com.pulselink.domain.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LinkDebugEntryPoint {
    fun contactRepository(): ContactRepository
    fun linkManager(): ContactLinkManager
    fun settingsRepository(): SettingsRepository
    fun messageRepository(): MessageRepository
}
