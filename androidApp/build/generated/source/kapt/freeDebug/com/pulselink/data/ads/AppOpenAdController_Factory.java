package com.pulselink.data.ads;

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
public final class AppOpenAdController_Factory implements Factory<AppOpenAdController> {
  private final Provider<Context> contextProvider;

  public AppOpenAdController_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AppOpenAdController get() {
    return newInstance(contextProvider.get());
  }

  public static AppOpenAdController_Factory create(Provider<Context> contextProvider) {
    return new AppOpenAdController_Factory(contextProvider);
  }

  public static AppOpenAdController newInstance(Context context) {
    return new AppOpenAdController(context);
  }
}
