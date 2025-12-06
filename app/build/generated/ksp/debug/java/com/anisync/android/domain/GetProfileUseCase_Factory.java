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
public final class GetProfileUseCase_Factory implements Factory<GetProfileUseCase> {
  private final Provider<ProfileRepository> repositoryProvider;

  private GetProfileUseCase_Factory(Provider<ProfileRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GetProfileUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static GetProfileUseCase_Factory create(Provider<ProfileRepository> repositoryProvider) {
    return new GetProfileUseCase_Factory(repositoryProvider);
  }

  public static GetProfileUseCase newInstance(ProfileRepository repository) {
    return new GetProfileUseCase(repository);
  }
}
