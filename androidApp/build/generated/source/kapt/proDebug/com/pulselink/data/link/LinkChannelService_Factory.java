package com.pulselink.data.link;

import com.google.firebase.firestore.FirebaseFirestore;
import com.pulselink.auth.FirebaseAuthManager;
import com.pulselink.domain.repository.ContactRepository;
import com.pulselink.domain.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
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
public final class LinkChannelService_Factory implements Factory<LinkChannelService> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<FirebaseAuthManager> authManagerProvider;

  public LinkChannelService_Factory(Provider<FirebaseFirestore> firestoreProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<FirebaseAuthManager> authManagerProvider) {
    this.firestoreProvider = firestoreProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.authManagerProvider = authManagerProvider;
  }

  @Override
  public LinkChannelService get() {
    return newInstance(firestoreProvider.get(), settingsRepositoryProvider.get(), contactRepositoryProvider.get(), authManagerProvider.get());
  }

  public static LinkChannelService_Factory create(Provider<FirebaseFirestore> firestoreProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<FirebaseAuthManager> authManagerProvider) {
    return new LinkChannelService_Factory(firestoreProvider, settingsRepositoryProvider, contactRepositoryProvider, authManagerProvider);
  }

  public static LinkChannelService newInstance(FirebaseFirestore firestore,
      SettingsRepository settingsRepository, ContactRepository contactRepository,
      FirebaseAuthManager authManager) {
    return new LinkChannelService(firestore, settingsRepository, contactRepository, authManager);
  }
}
