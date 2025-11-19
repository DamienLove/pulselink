package com.pulselink.data.alert;

import android.content.Context;
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
public final class NotificationRegistrar_Factory implements Factory<NotificationRegistrar> {
  private final Provider<Context> contextProvider;

  public NotificationRegistrar_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public NotificationRegistrar get() {
    return newInstance(contextProvider.get());
  }

  public static NotificationRegistrar_Factory create(Provider<Context> contextProvider) {
    return new NotificationRegistrar_Factory(contextProvider);
  }

  public static NotificationRegistrar newInstance(Context context) {
    return new NotificationRegistrar(context);
  }
}
