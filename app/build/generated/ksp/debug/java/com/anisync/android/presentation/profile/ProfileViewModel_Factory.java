package com.anisync.android.presentation.profile;

import com.anisync.android.domain.GetProfileUseCase;
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
public final class ProfileViewModel_Factory implements Factory<ProfileViewModel> {
  private final Provider<GetProfileUseCase> getProfileUseCaseProvider;

  private ProfileViewModel_Factory(Provider<GetProfileUseCase> getProfileUseCaseProvider) {
    this.getProfileUseCaseProvider = getProfileUseCaseProvider;
  }

  @Override
  public ProfileViewModel get() {
    return newInstance(getProfileUseCaseProvider.get());
  }

  public static ProfileViewModel_Factory create(
      Provider<GetProfileUseCase> getProfileUseCaseProvider) {
    return new ProfileViewModel_Factory(getProfileUseCaseProvider);
  }

  public static ProfileViewModel newInstance(GetProfileUseCase getProfileUseCase) {
    return new ProfileViewModel(getProfileUseCase);
  }
}
