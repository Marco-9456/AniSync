# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Google Tink classes
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn com.google.errorprone.annotations.InlineMe
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy

# Keep Protocol Buffer generated classes
-keep class com.anisync.android.data.proto.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }
