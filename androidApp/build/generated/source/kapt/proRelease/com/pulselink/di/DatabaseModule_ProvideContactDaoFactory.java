package com.pulselink.di;

import com.pulselink.data.db.ContactDao;
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
public final class DatabaseModule_ProvideContactDaoFactory implements Factory<ContactDao> {
  private final Provider<PulseLinkDatabase> databaseProvider;

  public DatabaseModule_ProvideContactDaoFactory(Provider<PulseLinkDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ContactDao get() {
    return provideContactDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideContactDaoFactory create(
      Provider<PulseLinkDatabase> databaseProvider) {
    return new DatabaseModule_ProvideContactDaoFactory(databaseProvider);
  }

  public static ContactDao provideContactDao(PulseLinkDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideContactDao(database));
  }
}
