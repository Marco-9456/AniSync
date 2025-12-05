package com.anisync.android.di;

import com.apollographql.apollo3.ApolloClient;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
    "KotlinInternalInJava"
})
public final class ApolloModule_ProvideApolloClientFactory implements Factory<ApolloClient> {
  private final Provider<AuthorizationInterceptor> authorizationInterceptorProvider;

  public ApolloModule_ProvideApolloClientFactory(
      Provider<AuthorizationInterceptor> authorizationInterceptorProvider) {
    this.authorizationInterceptorProvider = authorizationInterceptorProvider;
  }

  @Override
  public ApolloClient get() {
    return provideApolloClient(authorizationInterceptorProvider.get());
  }

  public static ApolloModule_ProvideApolloClientFactory create(
      Provider<AuthorizationInterceptor> authorizationInterceptorProvider) {
    return new ApolloModule_ProvideApolloClientFactory(authorizationInterceptorProvider);
  }

  public static ApolloClient provideApolloClient(
      AuthorizationInterceptor authorizationInterceptor) {
    return Preconditions.checkNotNullFromProvides(ApolloModule.INSTANCE.provideApolloClient(authorizationInterceptor));
  }
}
