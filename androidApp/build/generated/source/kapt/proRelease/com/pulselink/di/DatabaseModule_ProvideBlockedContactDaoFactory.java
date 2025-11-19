package com.pulselink.di;

import com.pulselink.data.db.BlockedContactDao;
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
public final class DatabaseModule_ProvideBlockedContactDaoFactory implements Factory<BlockedContactDao> {
  private final Provider<PulseLinkDatabase> databaseProvider;

  public DatabaseModule_ProvideBlockedContactDaoFactory(
      Provider<PulseLinkDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public BlockedContactDao get() {
    return provideBlockedContactDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideBlockedContactDaoFactory create(
      Provider<PulseLinkDatabase> databaseProvider) {
    return new DatabaseModule_ProvideBlockedContactDaoFactory(databaseProvider);
  }

  public static BlockedContactDao provideBlockedContactDao(PulseLinkDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideBlockedContactDao(database));
  }
}
