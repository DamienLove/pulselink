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
public final class MessageRepositoryImpl_Factory implements Factory<MessageRepositoryImpl> {
  private final Provider<ContactMessageDao> daoProvider;

  public MessageRepositoryImpl_Factory(Provider<ContactMessageDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public MessageRepositoryImpl get() {
    return newInstance(daoProvider.get());
  }

  public static MessageRepositoryImpl_Factory create(Provider<ContactMessageDao> daoProvider) {
    return new MessageRepositoryImpl_Factory(daoProvider);
  }

  public static MessageRepositoryImpl newInstance(ContactMessageDao dao) {
    return new MessageRepositoryImpl(dao);
  }
}
