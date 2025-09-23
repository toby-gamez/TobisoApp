# =================================
# PROGUARD/R8 OBFUSCATION RULES
# =================================

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }

# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep @androidx.compose.runtime.Composable class * { *; }
-keep @androidx.compose.runtime.Stable class * { *; }

# Keep Navigation Compose
-keep class androidx.navigation.** { *; }

# Keep Retrofit/Gson classes
-keep class com.google.gson.** { *; }
-keep class retrofit2.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep your application classes
-keep class com.tobiso.tobisoapp.** { *; }
-keep class com.example.tobisoappnative.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

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

# Suppress warnings
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn org.slf4j.**