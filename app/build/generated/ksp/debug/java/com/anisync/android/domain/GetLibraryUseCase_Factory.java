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
public final class GetLibraryUseCase_Factory implements Factory<GetLibraryUseCase> {
  private final Provider<LibraryRepository> repositoryProvider;

  public GetLibraryUseCase_Factory(Provider<LibraryRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GetLibraryUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static GetLibraryUseCase_Factory create(Provider<LibraryRepository> repositoryProvider) {
    return new GetLibraryUseCase_Factory(repositoryProvider);
  }

  public static GetLibraryUseCase newInstance(LibraryRepository repository) {
    return new GetLibraryUseCase(repository);
  }
}
