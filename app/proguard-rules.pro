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

# Optimize method calls
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# ========== Firebase ==========
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ========== Kotlin Coroutines ==========
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ========== Hilt ==========
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class ** extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ========== Compose ==========
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# ========== Lottie ==========
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ========== Coil (Image Loading) ==========
-keep class coil.** { *; }
-dontwarn coil.**

# ========== Data Classes (Keep for Firestore) ==========
# Keep all data classes with BOTH package names
-keep class com.example.habittracker.data.** { *; }
-keep class it.atraj.habittracker.data.** { *; }
-keep class com.example.habittracker.auth.User { *; }
-keep class it.atraj.habittracker.auth.User { *; }
-keepclassmembers class com.example.habittracker.data.** {
    *;
}
-keepclassmembers class it.atraj.habittracker.data.** {
    *;
}

# Keep all local data models (Habit, HabitAvatar, etc.)
-keep class it.atraj.habittracker.data.local.** { *; }
-keepclassmembers class it.atraj.habittracker.data.local.** {
    *;
}

# ========== UI Models and State Classes ==========
# Keep all UI state classes and their fields with BOTH package names
-keep class com.example.habittracker.ui.** { *; }
-keep class it.atraj.habittracker.ui.** { *; }
-keepclassmembers class com.example.habittracker.ui.** {
    *;
}
-keepclassmembers class it.atraj.habittracker.ui.** {
    *;
}

# Specifically keep important UI classes
-keep class it.atraj.habittracker.ui.HabitScreenState { *; }
-keep class it.atraj.habittracker.ui.AddHabitState { *; }
-keep class it.atraj.habittracker.ui.HabitCardUi { *; }
-keep class it.atraj.habittracker.ui.HabitUiModelsKt { *; }

# Keep all Kotlin data classes
-keep @kotlin.Metadata class * {
    *;
}
-keepclassmembers class * {
    @kotlin.jvm.JvmField *;
}

# Keep data class copy methods
-keepclassmembers class * {
    public ** copy(...);
    public ** component*();
}

# ========== Serialization ==========
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serialization for BOTH package names
-keep,includedescriptorclasses class com.example.habittracker.**$$serializer { *; }
-keep,includedescriptorclasses class it.atraj.habittracker.**$$serializer { *; }
-keepclassmembers class com.example.habittracker.** {
    *** Companion;
}
-keepclassmembers class it.atraj.habittracker.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.habittracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class it.atraj.habittracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ========== OkHttp ==========
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ========== Performance: Remove logging in release ==========
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
