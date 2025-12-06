package com.anisync.android.presentation.search;

import com.anisync.android.domain.SearchRepository;
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
public final class SearchViewModel_Factory implements Factory<SearchViewModel> {
  private final Provider<SearchRepository> searchRepositoryProvider;

  public SearchViewModel_Factory(Provider<SearchRepository> searchRepositoryProvider) {
    this.searchRepositoryProvider = searchRepositoryProvider;
  }

  @Override
  public SearchViewModel get() {
    return newInstance(searchRepositoryProvider.get());
  }

  public static SearchViewModel_Factory create(
      Provider<SearchRepository> searchRepositoryProvider) {
    return new SearchViewModel_Factory(searchRepositoryProvider);
  }

  public static SearchViewModel newInstance(SearchRepository searchRepository) {
    return new SearchViewModel(searchRepository);
  }
}
