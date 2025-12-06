package com.anisync.android.presentation.discover;

import com.anisync.android.domain.DiscoverRepository;
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
public final class DiscoverViewModel_Factory implements Factory<DiscoverViewModel> {
  private final Provider<DiscoverRepository> discoverRepositoryProvider;

  private DiscoverViewModel_Factory(Provider<DiscoverRepository> discoverRepositoryProvider) {
    this.discoverRepositoryProvider = discoverRepositoryProvider;
  }

  @Override
  public DiscoverViewModel get() {
    return newInstance(discoverRepositoryProvider.get());
  }

  public static DiscoverViewModel_Factory create(
      Provider<DiscoverRepository> discoverRepositoryProvider) {
    return new DiscoverViewModel_Factory(discoverRepositoryProvider);
  }

  public static DiscoverViewModel newInstance(DiscoverRepository discoverRepository) {
    return new DiscoverViewModel(discoverRepository);
  }
}
