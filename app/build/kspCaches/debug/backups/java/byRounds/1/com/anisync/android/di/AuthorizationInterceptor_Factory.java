package com.anisync.android.di;

import com.anisync.android.data.AuthRepository;
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
    "KotlinInternalInJava"
})
public final class AuthorizationInterceptor_Factory implements Factory<AuthorizationInterceptor> {
  private final Provider<AuthRepository> authRepositoryProvider;

  public AuthorizationInterceptor_Factory(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public AuthorizationInterceptor get() {
    return newInstance(authRepositoryProvider.get());
  }

  public static AuthorizationInterceptor_Factory create(
      Provider<AuthRepository> authRepositoryProvider) {
    return new AuthorizationInterceptor_Factory(authRepositoryProvider);
  }

  public static AuthorizationInterceptor newInstance(AuthRepository authRepository) {
    return new AuthorizationInterceptor(authRepository);
  }
}
