package com.pulselink.ui.state;

import com.pulselink.auth.FirebaseAuthManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class LoginViewModel_Factory implements Factory<LoginViewModel> {
  private final Provider<FirebaseAuthManager> authManagerProvider;

  public LoginViewModel_Factory(Provider<FirebaseAuthManager> authManagerProvider) {
    this.authManagerProvider = authManagerProvider;
  }

  @Override
  public LoginViewModel get() {
    return newInstance(authManagerProvider.get());
  }

  public static LoginViewModel_Factory create(Provider<FirebaseAuthManager> authManagerProvider) {
    return new LoginViewModel_Factory(authManagerProvider);
  }

  public static LoginViewModel newInstance(FirebaseAuthManager authManager) {
    return new LoginViewModel(authManager);
  }
}
