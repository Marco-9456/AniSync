package com.anisync.android.di;

import com.apollographql.apollo3.ApolloClient;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class ApolloModule_ProvideApolloClientFactory implements Factory<ApolloClient> {
  @Override
  public ApolloClient get() {
    return provideApolloClient();
  }

  public static ApolloModule_ProvideApolloClientFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ApolloClient provideApolloClient() {
    return Preconditions.checkNotNullFromProvides(ApolloModule.INSTANCE.provideApolloClient());
  }

  private static final class InstanceHolder {
    private static final ApolloModule_ProvideApolloClientFactory INSTANCE = new ApolloModule_ProvideApolloClientFactory();
  }
}
