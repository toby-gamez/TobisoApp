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

# =================================
# MEDIA3/EXOPLAYER RULES - KRITICKÉ PRO VIDEO
# =================================
-keep class androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ExoPlayer DataSource factories
-keep class * extends androidx.media3.datasource.DataSource$Factory { *; }
-keep class * implements androidx.media3.datasource.DataSource$Factory { *; }
-keep class androidx.media3.datasource.** { *; }

# Media3 decoder and extractor classes
-keep class androidx.media3.decoder.** { *; }
-keep class androidx.media3.extractor.** { *; }
-keep class androidx.media3.exoplayer.** { *; }

# =================================
# COIL RULES - KRITICKÉ PRO OBRÁZKY
# =================================
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# OkHttp (používá Coil i Retrofit)
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Okio (dependency pro OkHttp)
-keep class okio.** { *; }
-dontwarn okio.**

# =================================
# RICHTEXT/MARKDOWN RULES - KRITICKÉ PRO ZOBRAZENÍ OBRÁZKŮ
# =================================
-keep class com.halilibo.** { *; }
-keep interface com.halilibo.** { *; }
-dontwarn com.halilibo.**

# CommonMark markdown parser
-keep class org.commonmark.** { *; }
-keep interface org.commonmark.** { *; }
-dontwarn org.commonmark.**

# Markdown rendering a image loading
-keep class * extends org.commonmark.node.** { *; }
-keep class * implements org.commonmark.renderer.** { *; }


# Retrofit classes kept; project migrated to kotlinx.serialization
-keep class retrofit2.** { *; }

# Retrofit specific - keep response wrapper types
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations


# Keep model classes (serialization handled by kotlinx.serialization)
-keep class com.example.tobisoappnative.model.** { *; }


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


# Additional Retrofit/compat rules
-keep class sun.misc.Unsafe { *; }

# Keep all generic type information for API responses
# Ensure serialized field names are preserved for kotlinx.serialization
-keepattributes Signature,InnerClasses,EnclosingMethod
-keep class * { 
    @kotlinx.serialization.SerialName <fields>; 
}


# R8 full mode compatibility - keep serialized fields by name (kotlinx.serialization uses generated serializers)
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}

# =================================
# SECURITY RULES
# =================================

# Obfuscate sensitive configuration classes
-keep class com.example.tobisoappnative.config.SecurityConfig { 
    public *;
}
-keep class com.example.tobisoappnative.security.** { *; }

# Keep Android KeyStore functionality
-keep class android.security.keystore.** { *; }
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }

# Remove logging calls in production
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# String obfuscation for additional security
-adaptclassstrings

# Remove debug information
-keepattributes !LocalVariableTable,!LocalVariableTypeTable

# Optimize for production
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

# =================================
# SUPPRESS WARNINGS
# =================================

-dontwarn sun.misc.Unsafe
# Removed gson internal warnings after migrating to kotlinx.serialization
# =================================
# DODATEČNÁ PRAVIDLA PRO MEDIA
# =================================

# Prevent obfuscation of image loading and video playback classes
-keep class * extends android.view.View { *; }
-keep class * extends androidx.compose.ui.viewinterop.AndroidView { *; }

# Network security configuration
-keep class android.security.NetworkSecurityPolicy { *; }

# Image decoders and formatters
-keep class android.graphics.** { *; }
-keep class androidx.compose.ui.graphics.** { *; }

# WebView for potential web content
-keep class android.webkit.** { *; }

# Network related classes that might affect media loading
-keep class java.net.** { *; }
-keep class javax.net.** { *; }

# SSL/TLS classes for HTTPS media loading  
-keep class javax.net.ssl.** { *; }
-keep class java.security.cert.** { *; }

# Android media framework
-keep class android.media.** { *; }

# Prevent aggressive optimization that could break media loading
-keep class * implements java.io.Serializable { *; }