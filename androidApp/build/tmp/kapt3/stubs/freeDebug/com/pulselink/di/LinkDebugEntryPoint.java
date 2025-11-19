package com.pulselink.di;

import com.pulselink.data.link.ContactLinkManager;
import com.pulselink.domain.repository.ContactRepository;
import com.pulselink.domain.repository.MessageRepository;
import com.pulselink.domain.repository.SettingsRepository;
import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\bg\u0018\u00002\u00020\u0001J\b\u0010\u0002\u001a\u00020\u0003H&J\b\u0010\u0004\u001a\u00020\u0005H&J\b\u0010\u0006\u001a\u00020\u0007H&J\b\u0010\b\u001a\u00020\tH&\u00a8\u0006\n"}, d2 = {"Lcom/pulselink/di/LinkDebugEntryPoint;", "", "contactRepository", "Lcom/pulselink/domain/repository/ContactRepository;", "linkManager", "Lcom/pulselink/data/link/ContactLinkManager;", "messageRepository", "Lcom/pulselink/domain/repository/MessageRepository;", "settingsRepository", "Lcom/pulselink/domain/repository/SettingsRepository;", "androidApp_freeDebug"})
@dagger.hilt.EntryPoint()
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public abstract interface LinkDebugEntryPoint {
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.pulselink.domain.repository.ContactRepository contactRepository();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.pulselink.data.link.ContactLinkManager linkManager();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.pulselink.domain.repository.SettingsRepository settingsRepository();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.pulselink.domain.repository.MessageRepository messageRepository();
}