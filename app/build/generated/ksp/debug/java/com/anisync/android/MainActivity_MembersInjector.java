package com.anisync.android;

import com.anisync.android.data.AuthRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private MainActivity_MembersInjector(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectAuthRepository(instance, authRepositoryProvider.get());
  }

  public static MembersInjector<MainActivity> create(
      Provider<AuthRepository> authRepositoryProvider) {
    return new MainActivity_MembersInjector(authRepositoryProvider);
  }

  @InjectedFieldSignature("com.anisync.android.MainActivity.authRepository")
  public static void injectAuthRepository(MainActivity instance, AuthRepository authRepository) {
    instance.authRepository = authRepository;
  }
}
