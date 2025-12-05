package com.anisync.android.presentation.discover;

import com.anisync.android.domain.GetPopularAnimeUseCase;
import com.anisync.android.domain.GetTrendingAnimeUseCase;
import com.anisync.android.domain.GetUpcomingAnimeUseCase;
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
public final class DiscoverViewModel_Factory implements Factory<DiscoverViewModel> {
  private final Provider<GetTrendingAnimeUseCase> getTrendingAnimeUseCaseProvider;

  private final Provider<GetPopularAnimeUseCase> getPopularAnimeUseCaseProvider;

  private final Provider<GetUpcomingAnimeUseCase> getUpcomingAnimeUseCaseProvider;

  public DiscoverViewModel_Factory(
      Provider<GetTrendingAnimeUseCase> getTrendingAnimeUseCaseProvider,
      Provider<GetPopularAnimeUseCase> getPopularAnimeUseCaseProvider,
      Provider<GetUpcomingAnimeUseCase> getUpcomingAnimeUseCaseProvider) {
    this.getTrendingAnimeUseCaseProvider = getTrendingAnimeUseCaseProvider;
    this.getPopularAnimeUseCaseProvider = getPopularAnimeUseCaseProvider;
    this.getUpcomingAnimeUseCaseProvider = getUpcomingAnimeUseCaseProvider;
  }

  @Override
  public DiscoverViewModel get() {
    return newInstance(getTrendingAnimeUseCaseProvider.get(), getPopularAnimeUseCaseProvider.get(), getUpcomingAnimeUseCaseProvider.get());
  }

  public static DiscoverViewModel_Factory create(
      Provider<GetTrendingAnimeUseCase> getTrendingAnimeUseCaseProvider,
      Provider<GetPopularAnimeUseCase> getPopularAnimeUseCaseProvider,
      Provider<GetUpcomingAnimeUseCase> getUpcomingAnimeUseCaseProvider) {
    return new DiscoverViewModel_Factory(getTrendingAnimeUseCaseProvider, getPopularAnimeUseCaseProvider, getUpcomingAnimeUseCaseProvider);
  }

  public static DiscoverViewModel newInstance(GetTrendingAnimeUseCase getTrendingAnimeUseCase,
      GetPopularAnimeUseCase getPopularAnimeUseCase,
      GetUpcomingAnimeUseCase getUpcomingAnimeUseCase) {
    return new DiscoverViewModel(getTrendingAnimeUseCase, getPopularAnimeUseCase, getUpcomingAnimeUseCase);
  }
}
