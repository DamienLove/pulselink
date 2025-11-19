package com.pulselink.di;

import android.content.Context;
import android.os.Build;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.room.Room;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.pulselink.data.alert.AlertDispatcher;
import com.pulselink.data.alert.NotificationRegistrar;
import com.pulselink.data.alert.SoundCatalog;
import com.pulselink.data.beta.BetaAgreementRepositoryImpl;
import com.pulselink.data.db.AlertEventDao;
import com.pulselink.data.db.AlertRepositoryImpl;
import com.pulselink.data.db.BlockedContactDao;
import com.pulselink.data.db.BlockedContactRepositoryImpl;
import com.pulselink.data.db.ContactDao;
import com.pulselink.data.db.ContactRepositoryImpl;
import com.pulselink.data.db.ContactMessageDao;
import com.pulselink.data.db.MessageRepositoryImpl;
import com.pulselink.data.db.PulseLinkDatabase;
import com.pulselink.data.settings.SettingsRepositoryImpl;
import com.pulselink.domain.repository.AlertRepository;
import com.pulselink.domain.repository.BetaAgreementRepository;
import com.pulselink.domain.repository.BlockedContactRepository;
import com.pulselink.domain.repository.ContactRepository;
import com.pulselink.domain.repository.MessageRepository;
import com.pulselink.domain.repository.SettingsRepository;
import com.pulselink.util.AudioOverrideManager;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@dagger.Module()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b\'\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\'J\u0010\u0010\u0007\u001a\u00020\b2\u0006\u0010\u0005\u001a\u00020\tH\'J\u0010\u0010\n\u001a\u00020\u000b2\u0006\u0010\u0005\u001a\u00020\fH\'J\u0010\u0010\r\u001a\u00020\u000e2\u0006\u0010\u0005\u001a\u00020\u000fH\'J\u0010\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0005\u001a\u00020\u0012H\'J\u0010\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0005\u001a\u00020\u0015H\'\u00a8\u0006\u0016"}, d2 = {"Lcom/pulselink/di/RepositoryModule;", "", "()V", "bindAlertRepository", "Lcom/pulselink/domain/repository/AlertRepository;", "impl", "Lcom/pulselink/data/db/AlertRepositoryImpl;", "bindBetaAgreementRepository", "Lcom/pulselink/domain/repository/BetaAgreementRepository;", "Lcom/pulselink/data/beta/BetaAgreementRepositoryImpl;", "bindBlockedContactRepository", "Lcom/pulselink/domain/repository/BlockedContactRepository;", "Lcom/pulselink/data/db/BlockedContactRepositoryImpl;", "bindContactRepository", "Lcom/pulselink/domain/repository/ContactRepository;", "Lcom/pulselink/data/db/ContactRepositoryImpl;", "bindMessageRepository", "Lcom/pulselink/domain/repository/MessageRepository;", "Lcom/pulselink/data/db/MessageRepositoryImpl;", "bindSettingsRepository", "Lcom/pulselink/domain/repository/SettingsRepository;", "Lcom/pulselink/data/settings/SettingsRepositoryImpl;", "androidApp_proRelease"})
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public abstract class RepositoryModule {
    
    public RepositoryModule() {
        super();
    }
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public abstract com.pulselink.domain.repository.ContactRepository bindContactRepository(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.db.ContactRepositoryImpl impl);
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public abstract com.pulselink.domain.repository.AlertRepository bindAlertRepository(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.db.AlertRepositoryImpl impl);
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public abstract com.pulselink.domain.repository.SettingsRepository bindSettingsRepository(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.settings.SettingsRepositoryImpl impl);
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public abstract com.pulselink.domain.repository.MessageRepository bindMessageRepository(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.db.MessageRepositoryImpl impl);
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public abstract com.pulselink.domain.repository.BlockedContactRepository bindBlockedContactRepository(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.db.BlockedContactRepositoryImpl impl);
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public abstract com.pulselink.domain.repository.BetaAgreementRepository bindBetaAgreementRepository(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.beta.BetaAgreementRepositoryImpl impl);
}