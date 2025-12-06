package com.anisync.android.presentation.library;

import com.anisync.android.domain.LibraryRepository;
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
  private final Provider<LibraryRepository> libraryRepositoryProvider;

  public LibraryViewModel_Factory(Provider<LibraryRepository> libraryRepositoryProvider) {
    this.libraryRepositoryProvider = libraryRepositoryProvider;
  }

  @Override
  public LibraryViewModel get() {
    return newInstance(libraryRepositoryProvider.get());
  }

  public static LibraryViewModel_Factory create(
      Provider<LibraryRepository> libraryRepositoryProvider) {
    return new LibraryViewModel_Factory(libraryRepositoryProvider);
  }

  public static LibraryViewModel newInstance(LibraryRepository libraryRepository) {
    return new LibraryViewModel(libraryRepository);
  }
}
