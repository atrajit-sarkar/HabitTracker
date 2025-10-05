# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ========== Performance Optimizations ==========

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Optimize method calls - minimal optimization for smooth performance
-dontoptimize
-dontpreverify

# Prevent removal of used fields and methods
-keepclassmembers class * {
    *** get*();
    *** set*(...);
    *** is*();
}

# Keep data class generated methods
-keepclassmembers class ** {
    public *** component1();
    public *** component2();
    public *** component3();
    public *** component4();
    public *** component5();
    public *** component6();
    public *** component7();
}

# ========== Firebase ==========
# Only keep what's needed, let R8 optimize the rest
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Firestore
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.storage.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ========== Kotlin Coroutines ==========
-keep class kotlinx.coroutines.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ========== Hilt ==========
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class ** extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn dagger.hilt.**

# Keep all ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class androidx.lifecycle.** { *; }

# ========== Compose ==========
# Keep Compose runtime for smooth performance - DO NOT OPTIMIZE
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep animation classes for smooth animations
-keep class androidx.compose.animation.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }

# ========== Lottie ==========
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# Keep Accompanist for smooth effects
-keep class com.google.accompanist.** { *; }
-dontwarn com.google.accompanist.**

# ========== Coil (Image Loading) ==========
# Keep Coil completely for smooth image loading performance
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# ========== Data Classes (Keep for Firestore) ==========
# Keep data model classes completely for smooth operation
-keep class com.example.habittracker.data.** { *; }
-keep class it.atraj.habittracker.data.** { *; }
-keep class com.example.habittracker.auth.** { *; }
-keep class it.atraj.habittracker.auth.** { *; }

# Keep local data models
-keep class it.atraj.habittracker.data.local.** { *; }

# ========== UI Models and State Classes ==========
# Keep UI models completely to prevent lag
-keep class it.atraj.habittracker.ui.** { *; }
-keep class com.example.habittracker.ui.** { *; }

# Keep all repositories and use cases
-keep class it.atraj.habittracker.repository.** { *; }
-keep class com.example.habittracker.repository.** { *; }
-keep class it.atraj.habittracker.domain.** { *; }
-keep class com.example.habittracker.domain.** { *; }

# Keep all Kotlin data classes - minimal rules
-keepclassmembers @kotlin.Metadata class * {
    <fields>;
}

# Keep data class copy methods
-keepclassmembers class * {
    public ** copy(...);
}

# ========== Serialization ==========
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep serialization for data classes
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    <fields>;
    *** Companion;
}

# Keep Gson
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Room Database
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# ========== OkHttp ==========
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ========== Performance: Keep some logging for debugging ==========
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
