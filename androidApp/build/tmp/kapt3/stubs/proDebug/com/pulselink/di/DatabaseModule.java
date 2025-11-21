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
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0082\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J:\u0010\u0007\u001a\u00020\b2\b\b\u0001\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0014H\u0007J\u0010\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\u0010\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\u0010\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\u0018\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u001d0\u001c2\b\b\u0001\u0010\t\u001a\u00020\nH\u0007J\u0012\u0010\u001e\u001a\u00020\u00062\b\b\u0001\u0010\t\u001a\u00020\nH\u0007J\b\u0010\u001f\u001a\u00020 H\u0007J\b\u0010!\u001a\u00020\"H\u0007J\b\u0010#\u001a\u00020$H\u0007J\u0012\u0010%\u001a\u00020\u00102\b\b\u0001\u0010\t\u001a\u00020\nH\u0007J\u0012\u0010&\u001a\u00020\'2\b\b\u0001\u0010\t\u001a\u00020\nH\u0007J\u0012\u0010(\u001a\u00020\u00122\b\b\u0001\u0010\t\u001a\u00020\nH\u0007J\u0012\u0010)\u001a\u00020*2\b\b\u0001\u0010\t\u001a\u00020\nH\u0007\u00a8\u0006+"}, d2 = {"Lcom/pulselink/di/DatabaseModule;", "", "()V", "provideAlertDao", "Lcom/pulselink/data/db/AlertEventDao;", "database", "Lcom/pulselink/data/db/PulseLinkDatabase;", "provideAlertDispatcher", "Lcom/pulselink/data/alert/AlertDispatcher;", "context", "Landroid/content/Context;", "smsSender", "Lcom/pulselink/data/sms/SmsSender;", "locationProvider", "Lcom/pulselink/data/location/LocationProvider;", "registrar", "Lcom/pulselink/data/alert/NotificationRegistrar;", "soundCatalog", "Lcom/pulselink/data/alert/SoundCatalog;", "audioOverrideManager", "Lcom/pulselink/util/AudioOverrideManager;", "provideBlockedContactDao", "Lcom/pulselink/data/db/BlockedContactDao;", "provideContactDao", "Lcom/pulselink/data/db/ContactDao;", "provideContactMessageDao", "Lcom/pulselink/data/db/ContactMessageDao;", "provideDataStore", "Landroidx/datastore/core/DataStore;", "Landroidx/datastore/preferences/core/Preferences;", "provideDatabase", "provideFirebaseAuth", "Lcom/google/firebase/auth/FirebaseAuth;", "provideFirebaseFirestore", "Lcom/google/firebase/firestore/FirebaseFirestore;", "provideFirebaseFunctions", "Lcom/google/firebase/functions/FirebaseFunctions;", "provideNotificationRegistrar", "provideSmsManager", "Landroid/telephony/SmsManager;", "provideSoundCatalog", "provideTelephonyManager", "Landroid/telephony/TelephonyManager;", "androidApp_proDebug"})
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public final class DatabaseModule {
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.di.DatabaseModule INSTANCE = null;
    
    private DatabaseModule() {
        super();
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.data.db.PulseLinkDatabase provideDatabase(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @dagger.Provides()
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.data.db.ContactDao provideContactDao(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.db.PulseLinkDatabase database) {
        return null;
    }
    
    @dagger.Provides()
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.data.db.AlertEventDao provideAlertDao(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.db.PulseLinkDatabase database) {
        return null;
    }
    
    @dagger.Provides()
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.data.db.ContactMessageDao provideContactMessageDao(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.db.PulseLinkDatabase database) {
        return null;
    }
    
    @dagger.Provides()
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.data.db.BlockedContactDao provideBlockedContactDao(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.db.PulseLinkDatabase database) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> provideDataStore(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.data.alert.NotificationRegistrar provideNotificationRegistrar(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.data.alert.SoundCatalog provideSoundCatalog(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final android.telephony.SmsManager provideSmsManager(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.data.alert.AlertDispatcher provideAlertDispatcher(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.sms.SmsSender smsSender, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.location.LocationProvider locationProvider, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.alert.NotificationRegistrar registrar, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.alert.SoundCatalog soundCatalog, @org.jetbrains.annotations.NotNull()
    com.pulselink.util.AudioOverrideManager audioOverrideManager) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final android.telephony.TelephonyManager provideTelephonyManager(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.google.firebase.firestore.FirebaseFirestore provideFirebaseFirestore() {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.google.firebase.auth.FirebaseAuth provideFirebaseAuth() {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.google.firebase.functions.FirebaseFunctions provideFirebaseFunctions() {
        return null;
    }
}