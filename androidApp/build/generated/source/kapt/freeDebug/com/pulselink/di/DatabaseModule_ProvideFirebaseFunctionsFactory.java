package com.pulselink.di;

import com.google.firebase.functions.FirebaseFunctions;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class DatabaseModule_ProvideFirebaseFunctionsFactory implements Factory<FirebaseFunctions> {
  @Override
  public FirebaseFunctions get() {
    return provideFirebaseFunctions();
  }

  public static DatabaseModule_ProvideFirebaseFunctionsFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static FirebaseFunctions provideFirebaseFunctions() {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideFirebaseFunctions());
  }

  private static final class InstanceHolder {
    private static final DatabaseModule_ProvideFirebaseFunctionsFactory INSTANCE = new DatabaseModule_ProvideFirebaseFunctionsFactory();
  }
}
