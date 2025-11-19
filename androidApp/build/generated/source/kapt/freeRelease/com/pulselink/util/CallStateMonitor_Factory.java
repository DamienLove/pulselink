package com.pulselink.util;

import android.content.Context;
import android.telephony.TelephonyManager;
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
public final class CallStateMonitor_Factory implements Factory<CallStateMonitor> {
  private final Provider<Context> contextProvider;

  private final Provider<TelephonyManager> telephonyManagerProvider;

  public CallStateMonitor_Factory(Provider<Context> contextProvider,
      Provider<TelephonyManager> telephonyManagerProvider) {
    this.contextProvider = contextProvider;
    this.telephonyManagerProvider = telephonyManagerProvider;
  }

  @Override
  public CallStateMonitor get() {
    return newInstance(contextProvider.get(), telephonyManagerProvider.get());
  }

  public static CallStateMonitor_Factory create(Provider<Context> contextProvider,
      Provider<TelephonyManager> telephonyManagerProvider) {
    return new CallStateMonitor_Factory(contextProvider, telephonyManagerProvider);
  }

  public static CallStateMonitor newInstance(Context context, TelephonyManager telephonyManager) {
    return new CallStateMonitor(context, telephonyManager);
  }
}
