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
public final class ContactRepositoryImpl_Factory implements Factory<ContactRepositoryImpl> {
  private final Provider<ContactDao> contactDaoProvider;

  public ContactRepositoryImpl_Factory(Provider<ContactDao> contactDaoProvider) {
    this.contactDaoProvider = contactDaoProvider;
  }

  @Override
  public ContactRepositoryImpl get() {
    return newInstance(contactDaoProvider.get());
  }

  public static ContactRepositoryImpl_Factory create(Provider<ContactDao> contactDaoProvider) {
    return new ContactRepositoryImpl_Factory(contactDaoProvider);
  }

  public static ContactRepositoryImpl newInstance(ContactDao contactDao) {
    return new ContactRepositoryImpl(contactDao);
  }
}
