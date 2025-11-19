package com.pulselink.di;

import com.google.firebase.firestore.FirebaseFirestore;
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
public final class DatabaseModule_ProvideFirebaseFirestoreFactory implements Factory<FirebaseFirestore> {
  @Override
  public FirebaseFirestore get() {
    return provideFirebaseFirestore();
  }

  public static DatabaseModule_ProvideFirebaseFirestoreFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static FirebaseFirestore provideFirebaseFirestore() {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideFirebaseFirestore());
  }

  private static final class InstanceHolder {
    private static final DatabaseModule_ProvideFirebaseFirestoreFactory INSTANCE = new DatabaseModule_ProvideFirebaseFirestoreFactory();
  }
}
