package com.pulselink.data.location;

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
public final class LocationProvider_Factory implements Factory<LocationProvider> {
  private final Provider<Context> contextProvider;

  public LocationProvider_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public LocationProvider get() {
    return newInstance(contextProvider.get());
  }

  public static LocationProvider_Factory create(Provider<Context> contextProvider) {
    return new LocationProvider_Factory(contextProvider);
  }

  public static LocationProvider newInstance(Context context) {
    return new LocationProvider(context);
  }
}
