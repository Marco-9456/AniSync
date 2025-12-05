package com.anisync.android.domain;

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
public final class SearchMediaUseCase_Factory implements Factory<SearchMediaUseCase> {
  private final Provider<SearchRepository> repositoryProvider;

  public SearchMediaUseCase_Factory(Provider<SearchRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public SearchMediaUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static SearchMediaUseCase_Factory create(Provider<SearchRepository> repositoryProvider) {
    return new SearchMediaUseCase_Factory(repositoryProvider);
  }

  public static SearchMediaUseCase newInstance(SearchRepository repository) {
    return new SearchMediaUseCase(repository);
  }
}
