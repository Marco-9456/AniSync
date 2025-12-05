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
public final class ProfileRepositoryImpl_Factory implements Factory<ProfileRepositoryImpl> {
  private final Provider<ApolloClient> apolloClientProvider;

  public ProfileRepositoryImpl_Factory(Provider<ApolloClient> apolloClientProvider) {
    this.apolloClientProvider = apolloClientProvider;
  }

  @Override
  public ProfileRepositoryImpl get() {
    return newInstance(apolloClientProvider.get());
  }

  public static ProfileRepositoryImpl_Factory create(Provider<ApolloClient> apolloClientProvider) {
    return new ProfileRepositoryImpl_Factory(apolloClientProvider);
  }

  public static ProfileRepositoryImpl newInstance(ApolloClient apolloClient) {
    return new ProfileRepositoryImpl(apolloClient);
  }
}
