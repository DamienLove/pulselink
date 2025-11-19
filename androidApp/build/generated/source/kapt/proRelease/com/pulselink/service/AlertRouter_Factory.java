package com.pulselink.service;

import com.pulselink.data.alert.AlertDispatcher;
import com.pulselink.data.link.ContactLinkManager;
import com.pulselink.domain.repository.AlertRepository;
import com.pulselink.domain.repository.ContactRepository;
import com.pulselink.domain.repository.SettingsRepository;
import dagger.Lazy;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class AlertRouter_Factory implements Factory<AlertRouter> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<AlertRepository> alertRepositoryProvider;

  private final Provider<AlertDispatcher> dispatcherProvider;

  private final Provider<ContactLinkManager> contactLinkManagerProvider;

  public AlertRouter_Factory(Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<AlertRepository> alertRepositoryProvider,
      Provider<AlertDispatcher> dispatcherProvider,
      Provider<ContactLinkManager> contactLinkManagerProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.alertRepositoryProvider = alertRepositoryProvider;
    this.dispatcherProvider = dispatcherProvider;
    this.contactLinkManagerProvider = contactLinkManagerProvider;
  }

  @Override
  public AlertRouter get() {
    return newInstance(settingsRepositoryProvider.get(), contactRepositoryProvider.get(), alertRepositoryProvider.get(), dispatcherProvider.get(), DoubleCheck.lazy(contactLinkManagerProvider));
  }

  public static AlertRouter_Factory create(Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<AlertRepository> alertRepositoryProvider,
      Provider<AlertDispatcher> dispatcherProvider,
      Provider<ContactLinkManager> contactLinkManagerProvider) {
    return new AlertRouter_Factory(settingsRepositoryProvider, contactRepositoryProvider, alertRepositoryProvider, dispatcherProvider, contactLinkManagerProvider);
  }

  public static AlertRouter newInstance(SettingsRepository settingsRepository,
      ContactRepository contactRepository, AlertRepository alertRepository,
      AlertDispatcher dispatcher, Lazy<ContactLinkManager> contactLinkManager) {
    return new AlertRouter(settingsRepository, contactRepository, alertRepository, dispatcher, contactLinkManager);
  }
}
