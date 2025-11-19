package com.pulselink.data.db;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class AlertRepositoryImpl_Factory implements Factory<AlertRepositoryImpl> {
  private final Provider<AlertEventDao> alertEventDaoProvider;

  public AlertRepositoryImpl_Factory(Provider<AlertEventDao> alertEventDaoProvider) {
    this.alertEventDaoProvider = alertEventDaoProvider;
  }

  @Override
  public AlertRepositoryImpl get() {
    return newInstance(alertEventDaoProvider.get());
  }

  public static AlertRepositoryImpl_Factory create(Provider<AlertEventDao> alertEventDaoProvider) {
    return new AlertRepositoryImpl_Factory(alertEventDaoProvider);
  }

  public static AlertRepositoryImpl newInstance(AlertEventDao alertEventDao) {
    return new AlertRepositoryImpl(alertEventDao);
  }
}
