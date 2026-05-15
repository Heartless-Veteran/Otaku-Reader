# Add project specific ProGuard rules here.

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Room entities
-keep class app.otakureader.core.database.entity.** { *; }

# Keep serialization
-keep class kotlinx.serialization.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep custom Coil 3 Decoder implementations loaded reflectively by the ImageLoader pipeline.
# Without this, R8 would strip Factory/create() methods that Coil discovers at runtime.
-keep class * implements coil3.decode.Decoder { *; }
-keep class * implements coil3.decode.Decoder$Factory { *; }

# NOTE: Firebase/Firestore rules were removed — Firebase is not a project dependency.
# If Firebase is added in the future, re-add the appropriate keep rules.

# Keep Glance widget entry points and AppWidget subclasses
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep @dagger.hilt.EntryPoint interface * { *; }
