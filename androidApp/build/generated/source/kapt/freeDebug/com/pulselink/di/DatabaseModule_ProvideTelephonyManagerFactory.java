package com.pulselink.di;

import android.content.Context;
import android.telephony.TelephonyManager;
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
public final class DatabaseModule_ProvideTelephonyManagerFactory implements Factory<TelephonyManager> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvideTelephonyManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public TelephonyManager get() {
    return provideTelephonyManager(contextProvider.get());
  }

  public static DatabaseModule_ProvideTelephonyManagerFactory create(
      Provider<Context> contextProvider) {
    return new DatabaseModule_ProvideTelephonyManagerFactory(contextProvider);
  }

  public static TelephonyManager provideTelephonyManager(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideTelephonyManager(context));
  }
}
