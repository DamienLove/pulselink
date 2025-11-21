package com.pulselink.di;

import com.pulselink.data.db.AlertEventDao;
import com.pulselink.data.db.PulseLinkDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class DatabaseModule_ProvideAlertDaoFactory implements Factory<AlertEventDao> {
  private final Provider<PulseLinkDatabase> databaseProvider;

  public DatabaseModule_ProvideAlertDaoFactory(Provider<PulseLinkDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public AlertEventDao get() {
    return provideAlertDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideAlertDaoFactory create(
      Provider<PulseLinkDatabase> databaseProvider) {
    return new DatabaseModule_ProvideAlertDaoFactory(databaseProvider);
  }

  public static AlertEventDao provideAlertDao(PulseLinkDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideAlertDao(database));
  }
}
