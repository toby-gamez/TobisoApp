# =================================
# PROGUARD/R8 OBFUSCATION RULES
# =================================

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Compose runtime (targeted keeps)
-keep class * implements androidx.compose.runtime.Composable { *; }
-keep @androidx.compose.runtime.Stable class * { *; }

# Media3/ExoPlayer
-keep class androidx.media3.exoplayer.** { *; }
-keep class * extends androidx.media3.datasource.DataSource$Factory { *; }
-dontwarn androidx.media3.**

# Coil (targeted)
-keep class coil.request.ImageRequest$Builder { *; }
-keep class coil.size.** { *; }
-dontwarn coil.**

# OkHttp (used by Coil + Retrofit at native level)
-dontwarn okhttp3.**
-dontwarn okio.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}

# Retrofit
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class retrofit2.Call

# Application model classes (serialized)
-keep class com.tobiso.tobisoappnative.model.** { *; }

# Keep native methods
-keepclasseswithmembernames class * { native <methods>; }

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Aggressive obfuscation
-repackageclasses 'o'
-overloadaggressively
-allowaccessmodification

# Keep source file names for crash reports
-keepattributes SourceFile,LineNumberTable

# Security-related
-keep class com.tobiso.tobisoappnative.security.** { *; }
-keep class android.security.keystore.** { *; }

# Remove logging calls in production
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Optimize for production
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable