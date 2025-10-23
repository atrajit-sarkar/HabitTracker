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

# Aggressive R8 optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 7
-allowaccessmodification
-dontpreverify
-repackageclasses ''
-mergeinterfacesaggressively

# Enable more aggressive shrinking
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Strip Kotlin metadata where safe
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}

# Remove debug info
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkParameterIsNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkFieldIsNotNull(...);
    public static void checkReturnedValueIsNotNull(...);
}

# Optimize away debugging code
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Remove unused Compose internals
-assumenosideeffects class androidx.compose.runtime.ComposerKt {
    void sourceInformation(...);
    void sourceInformationMarkerStart(...);
    void sourceInformationMarkerEnd(...);
}

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
# Keep all data classes and their fields for proper serialization/deserialization
-keep class it.atraj.habittracker.data.** { *; }
-keep class it.atraj.habittracker.auth.** { *; }
-keepclassmembers class it.atraj.habittracker.data.** {
    public <init>(...);
    public <fields>;
}
-keepclassmembers class it.atraj.habittracker.auth.** {
    public <init>(...);
    public <fields>;
}

# Keep field names for Firestore reflection
-keepclassmembers class it.atraj.habittracker.data.local.Habit {
    <fields>;
}
-keepclassmembers class it.atraj.habittracker.data.local.HabitAvatar {
    <fields>;
}
-keepclassmembers class it.atraj.habittracker.data.firestore.** {
    <fields>;
}

# ========== Serialization ==========
-keepattributes *Annotation*, InnerClasses, Signature, Exception
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class it.atraj.habittracker.**$$serializer { *; }
-keepclassmembers class it.atraj.habittracker.** {
    *** Companion;
}
-keepclasseswithmembers class it.atraj.habittracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ========== OkHttp ==========
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep MainActivity fields accessed via reflection in UI (MusicSettingsScreen)
-keepclassmembers class it.atraj.habittracker.MainActivity {
    @androidx.annotation.Keep *;
    ** downloadManager;
    ** musicManager;
}

# ========== Ktor Client ==========
# Keep Ktor classes for YouTube downloader
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep Ktor plugins
-keep class io.ktor.client.plugins.** { *; }
-keep class io.ktor.client.engine.** { *; }
-keep class io.ktor.client.request.** { *; }

# SLF4J (logging framework used by Ktor)
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }

# ========== NewPipe Extractor ==========
# Keep NewPipe classes for YouTube extraction
-keep class org.schabi.newpipe.** { *; }
-keep class com.yushosei.newpipe.** { *; }
-keepclassmembers class org.schabi.newpipe.** { *; }
-keepclassmembers class com.yushosei.newpipe.** { *; }
-dontwarn org.schabi.newpipe.**
-dontwarn com.yushosei.newpipe.**

# Keep YouTube extractor classes
-keep class it.atraj.habittracker.youtube.** { *; }
-keepclassmembers class it.atraj.habittracker.youtube.** { *; }

# Mozilla Rhino (JavaScript engine used by NewPipe)
-dontwarn org.mozilla.javascript.**
-keep class org.mozilla.javascript.** { *; }

# Jsoup (HTML parser used by NewPipe)
-dontwarn org.jsoup.**
-keep class org.jsoup.** { *; }

# ========== Markwon (Markdown Rendering) ==========
# Keep Markwon classes for news rendering
-keep class io.noties.markwon.** { *; }
-keepclassmembers class io.noties.markwon.** { *; }
-dontwarn io.noties.markwon.**

# AndroidSVG (optional Markwon feature)
-dontwarn com.caverock.androidsvg.**

# CommonMark (optional Markwon feature)
-dontwarn org.commonmark.ext.gfm.strikethrough.**

# GIF support (optional Markwon feature)
-dontwarn pl.droidsonroids.gif.**

# Keep markdown parser classes
-keep class org.commonmark.** { *; }
-dontwarn org.commonmark.**
