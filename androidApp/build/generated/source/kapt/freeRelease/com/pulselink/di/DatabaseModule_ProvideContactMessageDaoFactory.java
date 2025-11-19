package com.pulselink.di;

import com.pulselink.data.db.ContactMessageDao;
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
public final class DatabaseModule_ProvideContactMessageDaoFactory implements Factory<ContactMessageDao> {
  private final Provider<PulseLinkDatabase> databaseProvider;

  public DatabaseModule_ProvideContactMessageDaoFactory(
      Provider<PulseLinkDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ContactMessageDao get() {
    return provideContactMessageDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideContactMessageDaoFactory create(
      Provider<PulseLinkDatabase> databaseProvider) {
    return new DatabaseModule_ProvideContactMessageDaoFactory(databaseProvider);
  }

  public static ContactMessageDao provideContactMessageDao(PulseLinkDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideContactMessageDao(database));
  }
}
