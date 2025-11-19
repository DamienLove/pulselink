package com.pulselink.di;

import android.content.Context;
import com.pulselink.data.alert.AlertDispatcher;
import com.pulselink.data.alert.NotificationRegistrar;
import com.pulselink.data.alert.SoundCatalog;
import com.pulselink.data.location.LocationProvider;
import com.pulselink.data.sms.SmsSender;
import com.pulselink.util.AudioOverrideManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class DatabaseModule_ProvideAlertDispatcherFactory implements Factory<AlertDispatcher> {
  private final Provider<Context> contextProvider;

  private final Provider<SmsSender> smsSenderProvider;

  private final Provider<LocationProvider> locationProvider;

  private final Provider<NotificationRegistrar> registrarProvider;

  private final Provider<SoundCatalog> soundCatalogProvider;

  private final Provider<AudioOverrideManager> audioOverrideManagerProvider;

  public DatabaseModule_ProvideAlertDispatcherFactory(Provider<Context> contextProvider,
      Provider<SmsSender> smsSenderProvider, Provider<LocationProvider> locationProvider,
      Provider<NotificationRegistrar> registrarProvider,
      Provider<SoundCatalog> soundCatalogProvider,
      Provider<AudioOverrideManager> audioOverrideManagerProvider) {
    this.contextProvider = contextProvider;
    this.smsSenderProvider = smsSenderProvider;
    this.locationProvider = locationProvider;
    this.registrarProvider = registrarProvider;
    this.soundCatalogProvider = soundCatalogProvider;
    this.audioOverrideManagerProvider = audioOverrideManagerProvider;
  }

  @Override
  public AlertDispatcher get() {
    return provideAlertDispatcher(contextProvider.get(), smsSenderProvider.get(), locationProvider.get(), registrarProvider.get(), soundCatalogProvider.get(), audioOverrideManagerProvider.get());
  }

  public static DatabaseModule_ProvideAlertDispatcherFactory create(
      Provider<Context> contextProvider, Provider<SmsSender> smsSenderProvider,
      Provider<LocationProvider> locationProvider,
      Provider<NotificationRegistrar> registrarProvider,
      Provider<SoundCatalog> soundCatalogProvider,
      Provider<AudioOverrideManager> audioOverrideManagerProvider) {
    return new DatabaseModule_ProvideAlertDispatcherFactory(contextProvider, smsSenderProvider, locationProvider, registrarProvider, soundCatalogProvider, audioOverrideManagerProvider);
  }

  public static AlertDispatcher provideAlertDispatcher(Context context, SmsSender smsSender,
      LocationProvider locationProvider, NotificationRegistrar registrar, SoundCatalog soundCatalog,
      AudioOverrideManager audioOverrideManager) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideAlertDispatcher(context, smsSender, locationProvider, registrar, soundCatalog, audioOverrideManager));
  }
}
