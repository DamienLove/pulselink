package com.pulselink.ui;

import com.pulselink.data.ads.AppOpenAdController;
import com.pulselink.util.CallStateMonitor;
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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<AppOpenAdController> appOpenAdControllerProvider;

  private final Provider<CallStateMonitor> callStateMonitorProvider;

  public MainActivity_MembersInjector(Provider<AppOpenAdController> appOpenAdControllerProvider,
      Provider<CallStateMonitor> callStateMonitorProvider) {
    this.appOpenAdControllerProvider = appOpenAdControllerProvider;
    this.callStateMonitorProvider = callStateMonitorProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<AppOpenAdController> appOpenAdControllerProvider,
      Provider<CallStateMonitor> callStateMonitorProvider) {
    return new MainActivity_MembersInjector(appOpenAdControllerProvider, callStateMonitorProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectAppOpenAdController(instance, appOpenAdControllerProvider.get());
    injectCallStateMonitor(instance, callStateMonitorProvider.get());
  }

  @InjectedFieldSignature("com.pulselink.ui.MainActivity.appOpenAdController")
  public static void injectAppOpenAdController(MainActivity instance,
      AppOpenAdController appOpenAdController) {
    instance.appOpenAdController = appOpenAdController;
  }

  @InjectedFieldSignature("com.pulselink.ui.MainActivity.callStateMonitor")
  public static void injectCallStateMonitor(MainActivity instance,
      CallStateMonitor callStateMonitor) {
    instance.callStateMonitor = callStateMonitor;
  }
}
