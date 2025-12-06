package com.anisync.android.presentation.details;

import androidx.lifecycle.SavedStateHandle;
import com.anisync.android.domain.DetailsRepository;
import com.anisync.android.domain.GetMediaDetailsUseCase;
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
public final class DetailsViewModel_Factory implements Factory<DetailsViewModel> {
  private final Provider<GetMediaDetailsUseCase> getMediaDetailsUseCaseProvider;

  private final Provider<DetailsRepository> detailsRepositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private DetailsViewModel_Factory(Provider<GetMediaDetailsUseCase> getMediaDetailsUseCaseProvider,
      Provider<DetailsRepository> detailsRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.getMediaDetailsUseCaseProvider = getMediaDetailsUseCaseProvider;
    this.detailsRepositoryProvider = detailsRepositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public DetailsViewModel get() {
    return newInstance(getMediaDetailsUseCaseProvider.get(), detailsRepositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static DetailsViewModel_Factory create(
      Provider<GetMediaDetailsUseCase> getMediaDetailsUseCaseProvider,
      Provider<DetailsRepository> detailsRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new DetailsViewModel_Factory(getMediaDetailsUseCaseProvider, detailsRepositoryProvider, savedStateHandleProvider);
  }

  public static DetailsViewModel newInstance(GetMediaDetailsUseCase getMediaDetailsUseCase,
      DetailsRepository detailsRepository, SavedStateHandle savedStateHandle) {
    return new DetailsViewModel(getMediaDetailsUseCase, detailsRepository, savedStateHandle);
  }
}
