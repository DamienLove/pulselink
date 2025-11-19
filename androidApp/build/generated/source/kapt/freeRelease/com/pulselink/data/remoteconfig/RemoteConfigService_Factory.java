package com.pulselink.data.remoteconfig;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class RemoteConfigService_Factory implements Factory<RemoteConfigService> {
  @Override
  public RemoteConfigService get() {
    return newInstance();
  }

  public static RemoteConfigService_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RemoteConfigService newInstance() {
    return new RemoteConfigService();
  }

  private static final class InstanceHolder {
    private static final RemoteConfigService_Factory INSTANCE = new RemoteConfigService_Factory();
  }
}
