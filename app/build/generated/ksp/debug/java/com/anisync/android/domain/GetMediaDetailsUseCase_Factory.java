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
public final class GetMediaDetailsUseCase_Factory implements Factory<GetMediaDetailsUseCase> {
  private final Provider<DetailsRepository> repositoryProvider;

  private GetMediaDetailsUseCase_Factory(Provider<DetailsRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GetMediaDetailsUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static GetMediaDetailsUseCase_Factory create(
      Provider<DetailsRepository> repositoryProvider) {
    return new GetMediaDetailsUseCase_Factory(repositoryProvider);
  }

  public static GetMediaDetailsUseCase newInstance(DetailsRepository repository) {
    return new GetMediaDetailsUseCase(repository);
  }
}
