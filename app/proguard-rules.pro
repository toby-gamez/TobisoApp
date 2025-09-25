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

# CRITICAL: Keep TypeToken and generic type information for Gson/Retrofit
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Retrofit specific - keep response wrapper types
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

# Keep model classes for Gson serialization/deserialization - VELMI DŮLEŽITÉ!
-keep class com.example.tobisoappnative.model.** { 
    *; 
}

# Keep all fields and methods in model classes
-keepclassmembers class com.example.tobisoappnative.model.** {
    <fields>;
    <methods>;
}

# Keep SerializedName annotations
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep generic signatures for Gson - KRITICKÉ PRO RETROFIT
-keepattributes Signature

# Retrofit interface methods
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep Response/Call generic types
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class retrofit2.Call

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

# Additional Gson/Retrofit R8 compatibility rules
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.internal.bind.** { *; }

# Prevent R8 from optimizing away TypeToken usage in Retrofit
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keep class com.google.gson.internal.$Gson$Types { *; }
-keep class com.google.gson.internal.ParameterizedTypeHandlerMap { *; }

# Keep all generic type information for API responses
-keepattributes Signature,InnerClasses,EnclosingMethod
-keep class * { 
    @com.google.gson.annotations.SerializedName <fields>; 
}

# R8 full mode compatibility - more aggressive rules
-keepclassmembers class ** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Disable R8 optimization for problematic classes
-keep,allowobfuscation,allowoptimization class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowoptimization class com.google.gson.internal.LinkedTreeMap

# Suppress warnings
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn org.slf4j.**
-dontwarn sun.misc.Unsafe
-dontwarn com.google.gson.internal.**