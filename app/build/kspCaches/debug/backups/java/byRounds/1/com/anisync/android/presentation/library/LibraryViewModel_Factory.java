package com.anisync.android.presentation.library;

import com.anisync.android.domain.GetLibraryUseCase;
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
public final class LibraryViewModel_Factory implements Factory<LibraryViewModel> {
  private final Provider<GetLibraryUseCase> getLibraryUseCaseProvider;

  public LibraryViewModel_Factory(Provider<GetLibraryUseCase> getLibraryUseCaseProvider) {
    this.getLibraryUseCaseProvider = getLibraryUseCaseProvider;
  }

  @Override
  public LibraryViewModel get() {
    return newInstance(getLibraryUseCaseProvider.get());
  }

  public static LibraryViewModel_Factory create(
      Provider<GetLibraryUseCase> getLibraryUseCaseProvider) {
    return new LibraryViewModel_Factory(getLibraryUseCaseProvider);
  }

  public static LibraryViewModel newInstance(GetLibraryUseCase getLibraryUseCase) {
    return new LibraryViewModel(getLibraryUseCase);
  }
}
