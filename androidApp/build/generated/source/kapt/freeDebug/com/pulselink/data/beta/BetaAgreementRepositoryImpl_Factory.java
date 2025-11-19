package com.pulselink.data.beta;

import com.google.firebase.firestore.FirebaseFirestore;
import com.pulselink.auth.FirebaseAuthManager;
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
public final class BetaAgreementRepositoryImpl_Factory implements Factory<BetaAgreementRepositoryImpl> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<FirebaseAuthManager> authManagerProvider;

  public BetaAgreementRepositoryImpl_Factory(Provider<FirebaseFirestore> firestoreProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<FirebaseAuthManager> authManagerProvider) {
    this.firestoreProvider = firestoreProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.authManagerProvider = authManagerProvider;
  }

  @Override
  public BetaAgreementRepositoryImpl get() {
    return newInstance(firestoreProvider.get(), settingsRepositoryProvider.get(), authManagerProvider.get());
  }

  public static BetaAgreementRepositoryImpl_Factory create(
      Provider<FirebaseFirestore> firestoreProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<FirebaseAuthManager> authManagerProvider) {
    return new BetaAgreementRepositoryImpl_Factory(firestoreProvider, settingsRepositoryProvider, authManagerProvider);
  }

  public static BetaAgreementRepositoryImpl newInstance(FirebaseFirestore firestore,
      SettingsRepository settingsRepository, FirebaseAuthManager authManager) {
    return new BetaAgreementRepositoryImpl(firestore, settingsRepository, authManager);
  }
}
