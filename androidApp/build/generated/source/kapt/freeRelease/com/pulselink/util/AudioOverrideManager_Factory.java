package com.pulselink.util;

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
public final class AudioOverrideManager_Factory implements Factory<AudioOverrideManager> {
  private final Provider<Context> contextProvider;

  public AudioOverrideManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AudioOverrideManager get() {
    return newInstance(contextProvider.get());
  }

  public static AudioOverrideManager_Factory create(Provider<Context> contextProvider) {
    return new AudioOverrideManager_Factory(contextProvider);
  }

  public static AudioOverrideManager newInstance(Context context) {
    return new AudioOverrideManager(context);
  }
}
