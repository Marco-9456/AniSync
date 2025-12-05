package com.anisync.android.data;

import com.apollographql.apollo3.ApolloClient;
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
public final class DiscoverRepositoryImpl_Factory implements Factory<DiscoverRepositoryImpl> {
  private final Provider<ApolloClient> apolloClientProvider;

  public DiscoverRepositoryImpl_Factory(Provider<ApolloClient> apolloClientProvider) {
    this.apolloClientProvider = apolloClientProvider;
  }

  @Override
  public DiscoverRepositoryImpl get() {
    return newInstance(apolloClientProvider.get());
  }

  public static DiscoverRepositoryImpl_Factory create(Provider<ApolloClient> apolloClientProvider) {
    return new DiscoverRepositoryImpl_Factory(apolloClientProvider);
  }

  public static DiscoverRepositoryImpl newInstance(ApolloClient apolloClient) {
    return new DiscoverRepositoryImpl(apolloClient);
  }
}
