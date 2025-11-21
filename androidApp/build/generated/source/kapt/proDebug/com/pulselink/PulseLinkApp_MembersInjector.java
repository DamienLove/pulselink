package com.pulselink;

import androidx.hilt.work.HiltWorkerFactory;
import com.pulselink.auth.FirebaseAuthManager;
import com.pulselink.data.ads.AppOpenAdController;
import com.pulselink.data.remoteconfig.RemoteConfigService;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class PulseLinkApp_MembersInjector implements MembersInjector<PulseLinkApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  private final Provider<AppOpenAdController> appOpenAdControllerProvider;

  private final Provider<FirebaseAuthManager> firebaseAuthManagerProvider;

  private final Provider<RemoteConfigService> remoteConfigServiceProvider;

  public PulseLinkApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider,
      Provider<AppOpenAdController> appOpenAdControllerProvider,
      Provider<FirebaseAuthManager> firebaseAuthManagerProvider,
      Provider<RemoteConfigService> remoteConfigServiceProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
    this.appOpenAdControllerProvider = appOpenAdControllerProvider;
    this.firebaseAuthManagerProvider = firebaseAuthManagerProvider;
    this.remoteConfigServiceProvider = remoteConfigServiceProvider;
  }

  public static MembersInjector<PulseLinkApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider,
      Provider<AppOpenAdController> appOpenAdControllerProvider,
      Provider<FirebaseAuthManager> firebaseAuthManagerProvider,
      Provider<RemoteConfigService> remoteConfigServiceProvider) {
    return new PulseLinkApp_MembersInjector(workerFactoryProvider, appOpenAdControllerProvider, firebaseAuthManagerProvider, remoteConfigServiceProvider);
  }

  @Override
  public void injectMembers(PulseLinkApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
    injectAppOpenAdController(instance, appOpenAdControllerProvider.get());
    injectFirebaseAuthManager(instance, firebaseAuthManagerProvider.get());
    injectRemoteConfigService(instance, remoteConfigServiceProvider.get());
  }

  @InjectedFieldSignature("com.pulselink.PulseLinkApp.workerFactory")
  public static void injectWorkerFactory(PulseLinkApp instance, HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }

  @InjectedFieldSignature("com.pulselink.PulseLinkApp.appOpenAdController")
  public static void injectAppOpenAdController(PulseLinkApp instance,
      AppOpenAdController appOpenAdController) {
    instance.appOpenAdController = appOpenAdController;
  }

  @InjectedFieldSignature("com.pulselink.PulseLinkApp.firebaseAuthManager")
  public static void injectFirebaseAuthManager(PulseLinkApp instance,
      FirebaseAuthManager firebaseAuthManager) {
    instance.firebaseAuthManager = firebaseAuthManager;
  }

  @InjectedFieldSignature("com.pulselink.PulseLinkApp.remoteConfigService")
  public static void injectRemoteConfigService(PulseLinkApp instance,
      RemoteConfigService remoteConfigService) {
    instance.remoteConfigService = remoteConfigService;
  }
}
