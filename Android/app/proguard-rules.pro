# Add project specific ProGuard rules here.

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Compose / Kotlin
-keep class com.touchbridge.mobile.** { *; }
