package com.pulselink.di;

import android.content.Context;
import com.pulselink.data.alert.SoundCatalog;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideSoundCatalogFactory implements Factory<SoundCatalog> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvideSoundCatalogFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SoundCatalog get() {
    return provideSoundCatalog(contextProvider.get());
  }

  public static DatabaseModule_ProvideSoundCatalogFactory create(
      Provider<Context> contextProvider) {
    return new DatabaseModule_ProvideSoundCatalogFactory(contextProvider);
  }

  public static SoundCatalog provideSoundCatalog(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideSoundCatalog(context));
  }
}
