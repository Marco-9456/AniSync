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
public final class GetTrendingAnimeUseCase_Factory implements Factory<GetTrendingAnimeUseCase> {
  private final Provider<DiscoverRepository> repositoryProvider;

  public GetTrendingAnimeUseCase_Factory(Provider<DiscoverRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GetTrendingAnimeUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static GetTrendingAnimeUseCase_Factory create(
      Provider<DiscoverRepository> repositoryProvider) {
    return new GetTrendingAnimeUseCase_Factory(repositoryProvider);
  }

  public static GetTrendingAnimeUseCase newInstance(DiscoverRepository repository) {
    return new GetTrendingAnimeUseCase(repository);
  }
}
