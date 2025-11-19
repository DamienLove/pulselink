package com.pulselink.di;

import android.content.Context;
import android.telephony.SmsManager;
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
public final class DatabaseModule_ProvideSmsManagerFactory implements Factory<SmsManager> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvideSmsManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SmsManager get() {
    return provideSmsManager(contextProvider.get());
  }

  public static DatabaseModule_ProvideSmsManagerFactory create(Provider<Context> contextProvider) {
    return new DatabaseModule_ProvideSmsManagerFactory(contextProvider);
  }

  public static SmsManager provideSmsManager(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideSmsManager(context));
  }
}
