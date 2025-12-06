package com.anisync.android.domain;

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
public final class GetPopularAnimeUseCase_Factory implements Factory<GetPopularAnimeUseCase> {
  private final Provider<DiscoverRepository> repositoryProvider;

  private GetPopularAnimeUseCase_Factory(Provider<DiscoverRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GetPopularAnimeUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static GetPopularAnimeUseCase_Factory create(
      Provider<DiscoverRepository> repositoryProvider) {
    return new GetPopularAnimeUseCase_Factory(repositoryProvider);
  }

  public static GetPopularAnimeUseCase newInstance(DiscoverRepository repository) {
    return new GetPopularAnimeUseCase(repository);
  }
}
