package com.pulselink.data.link;

import android.content.Context;
import com.pulselink.data.alert.NotificationRegistrar;
import com.pulselink.data.alert.SoundCatalog;
import com.pulselink.domain.repository.SettingsRepository;
import com.pulselink.service.AlertRouter;
import com.pulselink.util.AudioOverrideManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class RemoteActionHandler_Factory implements Factory<RemoteActionHandler> {
  private final Provider<Context> contextProvider;

  private final Provider<AlertRouter> alertRouterProvider;

  private final Provider<AudioOverrideManager> audioOverrideManagerProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<NotificationRegistrar> notificationRegistrarProvider;

  private final Provider<SoundCatalog> soundCatalogProvider;

  public RemoteActionHandler_Factory(Provider<Context> contextProvider,
      Provider<AlertRouter> alertRouterProvider,
      Provider<AudioOverrideManager> audioOverrideManagerProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<NotificationRegistrar> notificationRegistrarProvider,
      Provider<SoundCatalog> soundCatalogProvider) {
    this.contextProvider = contextProvider;
    this.alertRouterProvider = alertRouterProvider;
    this.audioOverrideManagerProvider = audioOverrideManagerProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.notificationRegistrarProvider = notificationRegistrarProvider;
    this.soundCatalogProvider = soundCatalogProvider;
  }

  @Override
  public RemoteActionHandler get() {
    return newInstance(contextProvider.get(), alertRouterProvider.get(), audioOverrideManagerProvider.get(), settingsRepositoryProvider.get(), notificationRegistrarProvider.get(), soundCatalogProvider.get());
  }

  public static RemoteActionHandler_Factory create(Provider<Context> contextProvider,
      Provider<AlertRouter> alertRouterProvider,
      Provider<AudioOverrideManager> audioOverrideManagerProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<NotificationRegistrar> notificationRegistrarProvider,
      Provider<SoundCatalog> soundCatalogProvider) {
    return new RemoteActionHandler_Factory(contextProvider, alertRouterProvider, audioOverrideManagerProvider, settingsRepositoryProvider, notificationRegistrarProvider, soundCatalogProvider);
  }

  public static RemoteActionHandler newInstance(Context context, AlertRouter alertRouter,
      AudioOverrideManager audioOverrideManager, SettingsRepository settingsRepository,
      NotificationRegistrar notificationRegistrar, SoundCatalog soundCatalog) {
    return new RemoteActionHandler(context, alertRouter, audioOverrideManager, settingsRepository, notificationRegistrar, soundCatalog);
  }
}
