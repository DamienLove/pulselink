package com.pulselink.data.alert;

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
public final class SoundCatalog_Factory implements Factory<SoundCatalog> {
  private final Provider<Context> contextProvider;

  public SoundCatalog_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SoundCatalog get() {
    return newInstance(contextProvider.get());
  }

  public static SoundCatalog_Factory create(Provider<Context> contextProvider) {
    return new SoundCatalog_Factory(contextProvider);
  }

  public static SoundCatalog newInstance(Context context) {
    return new SoundCatalog(context);
  }
}
