package com.pulselink.data.assistant;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.pulselink.auth.FirebaseAuthManager;
import com.pulselink.data.link.ContactLinkManager;
import com.pulselink.domain.repository.ContactRepository;
import com.pulselink.service.AlertRouter;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class NaturalLanguageCommandProcessor_Factory implements Factory<NaturalLanguageCommandProcessor> {
  private final Provider<Context> contextProvider;

  private final Provider<FirebaseFunctions> functionsProvider;

  private final Provider<FirebaseAuth> authProvider;

  private final Provider<AlertRouter> alertRouterProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<ContactLinkManager> linkManagerProvider;

  private final Provider<FirebaseAuthManager> firebaseAuthManagerProvider;

  public NaturalLanguageCommandProcessor_Factory(Provider<Context> contextProvider,
      Provider<FirebaseFunctions> functionsProvider, Provider<FirebaseAuth> authProvider,
      Provider<AlertRouter> alertRouterProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<ContactLinkManager> linkManagerProvider,
      Provider<FirebaseAuthManager> firebaseAuthManagerProvider) {
    this.contextProvider = contextProvider;
    this.functionsProvider = functionsProvider;
    this.authProvider = authProvider;
    this.alertRouterProvider = alertRouterProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.linkManagerProvider = linkManagerProvider;
    this.firebaseAuthManagerProvider = firebaseAuthManagerProvider;
  }

  @Override
  public NaturalLanguageCommandProcessor get() {
    return newInstance(contextProvider.get(), functionsProvider.get(), authProvider.get(), alertRouterProvider.get(), contactRepositoryProvider.get(), linkManagerProvider.get(), firebaseAuthManagerProvider.get());
  }

  public static NaturalLanguageCommandProcessor_Factory create(Provider<Context> contextProvider,
      Provider<FirebaseFunctions> functionsProvider, Provider<FirebaseAuth> authProvider,
      Provider<AlertRouter> alertRouterProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<ContactLinkManager> linkManagerProvider,
      Provider<FirebaseAuthManager> firebaseAuthManagerProvider) {
    return new NaturalLanguageCommandProcessor_Factory(contextProvider, functionsProvider, authProvider, alertRouterProvider, contactRepositoryProvider, linkManagerProvider, firebaseAuthManagerProvider);
  }

  public static NaturalLanguageCommandProcessor newInstance(Context context,
      FirebaseFunctions functions, FirebaseAuth auth, AlertRouter alertRouter,
      ContactRepository contactRepository, ContactLinkManager linkManager,
      FirebaseAuthManager firebaseAuthManager) {
    return new NaturalLanguageCommandProcessor(context, functions, auth, alertRouter, contactRepository, linkManager, firebaseAuthManager);
  }
}
