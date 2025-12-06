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
public final class LibraryRepositoryImpl_Factory implements Factory<LibraryRepositoryImpl> {
  private final Provider<ApolloClient> apolloClientProvider;

  private LibraryRepositoryImpl_Factory(Provider<ApolloClient> apolloClientProvider) {
    this.apolloClientProvider = apolloClientProvider;
  }

  @Override
  public LibraryRepositoryImpl get() {
    return newInstance(apolloClientProvider.get());
  }

  public static LibraryRepositoryImpl_Factory create(Provider<ApolloClient> apolloClientProvider) {
    return new LibraryRepositoryImpl_Factory(apolloClientProvider);
  }

  public static LibraryRepositoryImpl newInstance(ApolloClient apolloClient) {
    return new LibraryRepositoryImpl(apolloClient);
  }
}
