package com.pulselink.ui.state;

import com.pulselink.auth.FirebaseAuthManager;
import com.pulselink.data.alert.SoundCatalog;
import com.pulselink.data.assistant.NaturalLanguageCommandProcessor;
import com.pulselink.data.link.ContactLinkManager;
import com.pulselink.domain.repository.AlertRepository;
import com.pulselink.domain.repository.BetaAgreementRepository;
import com.pulselink.domain.repository.BlockedContactRepository;
import com.pulselink.domain.repository.ContactRepository;
import com.pulselink.domain.repository.MessageRepository;
import com.pulselink.domain.repository.SettingsRepository;
import com.pulselink.service.AlertRouter;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class MainViewModel_Factory implements Factory<MainViewModel> {
  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<AlertRepository> alertRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<AlertRouter> alertRouterProvider;

  private final Provider<SoundCatalog> soundCatalogProvider;

  private final Provider<ContactLinkManager> linkManagerProvider;

  private final Provider<MessageRepository> messageRepositoryProvider;

  private final Provider<BlockedContactRepository> blockedContactRepositoryProvider;

  private final Provider<BetaAgreementRepository> betaAgreementRepositoryProvider;

  private final Provider<NaturalLanguageCommandProcessor> naturalLanguageCommandProcessorProvider;

  private final Provider<FirebaseAuthManager> firebaseAuthManagerProvider;

  public MainViewModel_Factory(Provider<ContactRepository> contactRepositoryProvider,
      Provider<AlertRepository> alertRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AlertRouter> alertRouterProvider, Provider<SoundCatalog> soundCatalogProvider,
      Provider<ContactLinkManager> linkManagerProvider,
      Provider<MessageRepository> messageRepositoryProvider,
      Provider<BlockedContactRepository> blockedContactRepositoryProvider,
      Provider<BetaAgreementRepository> betaAgreementRepositoryProvider,
      Provider<NaturalLanguageCommandProcessor> naturalLanguageCommandProcessorProvider,
      Provider<FirebaseAuthManager> firebaseAuthManagerProvider) {
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.alertRepositoryProvider = alertRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.alertRouterProvider = alertRouterProvider;
    this.soundCatalogProvider = soundCatalogProvider;
    this.linkManagerProvider = linkManagerProvider;
    this.messageRepositoryProvider = messageRepositoryProvider;
    this.blockedContactRepositoryProvider = blockedContactRepositoryProvider;
    this.betaAgreementRepositoryProvider = betaAgreementRepositoryProvider;
    this.naturalLanguageCommandProcessorProvider = naturalLanguageCommandProcessorProvider;
    this.firebaseAuthManagerProvider = firebaseAuthManagerProvider;
  }

  @Override
  public MainViewModel get() {
    return newInstance(contactRepositoryProvider.get(), alertRepositoryProvider.get(), settingsRepositoryProvider.get(), alertRouterProvider.get(), soundCatalogProvider.get(), linkManagerProvider.get(), messageRepositoryProvider.get(), blockedContactRepositoryProvider.get(), betaAgreementRepositoryProvider.get(), naturalLanguageCommandProcessorProvider.get(), firebaseAuthManagerProvider.get());
  }

  public static MainViewModel_Factory create(Provider<ContactRepository> contactRepositoryProvider,
      Provider<AlertRepository> alertRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AlertRouter> alertRouterProvider, Provider<SoundCatalog> soundCatalogProvider,
      Provider<ContactLinkManager> linkManagerProvider,
      Provider<MessageRepository> messageRepositoryProvider,
      Provider<BlockedContactRepository> blockedContactRepositoryProvider,
      Provider<BetaAgreementRepository> betaAgreementRepositoryProvider,
      Provider<NaturalLanguageCommandProcessor> naturalLanguageCommandProcessorProvider,
      Provider<FirebaseAuthManager> firebaseAuthManagerProvider) {
    return new MainViewModel_Factory(contactRepositoryProvider, alertRepositoryProvider, settingsRepositoryProvider, alertRouterProvider, soundCatalogProvider, linkManagerProvider, messageRepositoryProvider, blockedContactRepositoryProvider, betaAgreementRepositoryProvider, naturalLanguageCommandProcessorProvider, firebaseAuthManagerProvider);
  }

  public static MainViewModel newInstance(ContactRepository contactRepository,
      AlertRepository alertRepository, SettingsRepository settingsRepository,
      AlertRouter alertRouter, SoundCatalog soundCatalog, ContactLinkManager linkManager,
      MessageRepository messageRepository, BlockedContactRepository blockedContactRepository,
      BetaAgreementRepository betaAgreementRepository,
      NaturalLanguageCommandProcessor naturalLanguageCommandProcessor,
      FirebaseAuthManager firebaseAuthManager) {
    return new MainViewModel(contactRepository, alertRepository, settingsRepository, alertRouter, soundCatalog, linkManager, messageRepository, blockedContactRepository, betaAgreementRepository, naturalLanguageCommandProcessor, firebaseAuthManager);
  }
}
