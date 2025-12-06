package com.anisync.android.data;

import com.apollographql.apollo3.ApolloClient;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class DetailsRepositoryImpl_Factory implements Factory<DetailsRepositoryImpl> {
  private final Provider<ApolloClient> apolloClientProvider;

  private DetailsRepositoryImpl_Factory(Provider<ApolloClient> apolloClientProvider) {
    this.apolloClientProvider = apolloClientProvider;
  }

  @Override
  public DetailsRepositoryImpl get() {
    return newInstance(apolloClientProvider.get());
  }

  public static DetailsRepositoryImpl_Factory create(Provider<ApolloClient> apolloClientProvider) {
    return new DetailsRepositoryImpl_Factory(apolloClientProvider);
  }

  public static DetailsRepositoryImpl newInstance(ApolloClient apolloClient) {
    return new DetailsRepositoryImpl(apolloClient);
  }
}
