package com.pulselink.di

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.pulselink.data.alert.AlertDispatcher
import com.pulselink.data.alert.NotificationRegistrar
import com.pulselink.data.alert.SoundCatalog
import com.pulselink.data.beta.BetaAgreementRepositoryImpl
import com.pulselink.data.db.AlertEventDao
import com.pulselink.data.db.AlertRepositoryImpl
import com.pulselink.data.db.BlockedContactDao
import com.pulselink.data.db.BlockedContactRepositoryImpl
import com.pulselink.data.db.ContactDao
import com.pulselink.data.db.ContactRepositoryImpl
import com.pulselink.data.db.ContactMessageDao
import com.pulselink.data.db.MessageRepositoryImpl
import com.pulselink.data.db.PulseLinkDatabase
import com.pulselink.data.settings.SettingsRepositoryImpl
import com.pulselink.data.settings.provideSettingsDataStore
import com.pulselink.domain.repository.AlertRepository
import com.pulselink.domain.repository.BetaAgreementRepository
import com.pulselink.domain.repository.BlockedContactRepository
import com.pulselink.domain.repository.ContactRepository
import com.pulselink.domain.repository.MessageRepository
import com.pulselink.domain.repository.SettingsRepository
import com.pulselink.util.AudioOverrideManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository

    @Binds
    @Singleton
    abstract fun bindAlertRepository(impl: AlertRepositoryImpl): AlertRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @Binds
    @Singleton
    abstract fun bindBlockedContactRepository(impl: BlockedContactRepositoryImpl): BlockedContactRepository

    @Binds
    @Singleton
    abstract fun bindBetaAgreementRepository(impl: BetaAgreementRepositoryImpl): BetaAgreementRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PulseLinkDatabase =
        Room.databaseBuilder(context, PulseLinkDatabase::class.java, "pulselink.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideContactDao(database: PulseLinkDatabase): ContactDao = database.contactDao()

    @Provides
    fun provideAlertDao(database: PulseLinkDatabase): AlertEventDao = database.alertEventDao()

    @Provides
    fun provideContactMessageDao(database: PulseLinkDatabase): ContactMessageDao = database.contactMessageDao()

    @Provides
    fun provideBlockedContactDao(database: PulseLinkDatabase): BlockedContactDao = database.blockedContactDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        provideSettingsDataStore(context)

    @Provides
    @Singleton
    fun provideNotificationRegistrar(@ApplicationContext context: Context): NotificationRegistrar =
        NotificationRegistrar(context)

    @Provides
    @Singleton
    fun provideSoundCatalog(@ApplicationContext context: Context): SoundCatalog =
        SoundCatalog(context)

    @Provides
    @Singleton
    fun provideSmsManager(@ApplicationContext context: Context): SmsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
        } else {
            SmsManager.getDefault()
        }
    }

    @Provides
    @Singleton
    fun provideAlertDispatcher(
        @ApplicationContext context: Context,
        smsSender: com.pulselink.data.sms.SmsSender,
        locationProvider: com.pulselink.data.location.LocationProvider,
        registrar: NotificationRegistrar,
        soundCatalog: SoundCatalog,
        audioOverrideManager: AudioOverrideManager
    ): AlertDispatcher = AlertDispatcher(
        context = context,
        smsSender = smsSender,
        locationProvider = locationProvider,
        registrar = registrar,
        soundCatalog = soundCatalog,
        audioOverrideManager = audioOverrideManager
    )

    @Provides
    @Singleton
    fun provideTelephonyManager(@ApplicationContext context: Context): TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance()
}
