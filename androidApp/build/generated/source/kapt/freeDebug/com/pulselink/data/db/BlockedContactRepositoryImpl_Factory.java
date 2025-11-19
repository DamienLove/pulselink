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
public final class BlockedContactRepositoryImpl_Factory implements Factory<BlockedContactRepositoryImpl> {
  private final Provider<BlockedContactDao> blockedContactDaoProvider;

  public BlockedContactRepositoryImpl_Factory(
      Provider<BlockedContactDao> blockedContactDaoProvider) {
    this.blockedContactDaoProvider = blockedContactDaoProvider;
  }

  @Override
  public BlockedContactRepositoryImpl get() {
    return newInstance(blockedContactDaoProvider.get());
  }

  public static BlockedContactRepositoryImpl_Factory create(
      Provider<BlockedContactDao> blockedContactDaoProvider) {
    return new BlockedContactRepositoryImpl_Factory(blockedContactDaoProvider);
  }

  public static BlockedContactRepositoryImpl newInstance(BlockedContactDao blockedContactDao) {
    return new BlockedContactRepositoryImpl(blockedContactDao);
  }
}
