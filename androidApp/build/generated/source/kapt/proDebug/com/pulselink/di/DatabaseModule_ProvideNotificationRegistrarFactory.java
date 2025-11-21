package com.pulselink.di;

import android.content.Context;
import com.pulselink.data.alert.NotificationRegistrar;
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
public final class DatabaseModule_ProvideNotificationRegistrarFactory implements Factory<NotificationRegistrar> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvideNotificationRegistrarFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public NotificationRegistrar get() {
    return provideNotificationRegistrar(contextProvider.get());
  }

  public static DatabaseModule_ProvideNotificationRegistrarFactory create(
      Provider<Context> contextProvider) {
    return new DatabaseModule_ProvideNotificationRegistrarFactory(contextProvider);
  }

  public static NotificationRegistrar provideNotificationRegistrar(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideNotificationRegistrar(context));
  }
}
