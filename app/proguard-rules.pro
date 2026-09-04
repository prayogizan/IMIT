# ==============================================================================
# General Android & Kotlin Runtime Rules
# ==============================================================================
# Retain line numbers and source file attributes for readable deobfuscated crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve runtime annotations for Compose, Serialization, and Retrofit reflection
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ==============================================================================
# Koin DI (v4.2.2)
# ==============================================================================
-keep class org.koin.** { *; }
-dontwarn org.koin.**
-keep class io.insertkoin.** { *; }
-dontwarn io.insertkoin.**
# Preserve constructors instantiated via Koin reflection/DSL
-keepclassmembers class * {
    @org.koin.core.annotation.* <init>(...);
}

# ==============================================================================
# Kotlinx Serialization
# ==============================================================================
-dontnote kotlinx.serialization.SerializationKt
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.uncaan.imit.core.network.model.**$$serializer { *; }
-keepclassmembers class com.uncaan.imit.core.network.model.** { *** Companion; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    public static final ** Companion;
}
-keepclassmembers class * extends kotlinx.serialization.KSerializer {
    <init>(...);
}
-keep,allowobfuscation class com.uncaan.imit.core.model.** { *; }
-keep,allowobfuscation class com.uncaan.imit.core.network.model.** { *; }

# ==============================================================================
# Retrofit 3.0 & OkHttp 5.5
# ==============================================================================
-keep class retrofit2.** { *; }
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# ==============================================================================
# Room 3.x (androidx.room3.* & androidx.room.*)
# ==============================================================================
-dontwarn androidx.room.**
-dontwarn androidx.room3.**
-keep class * extends androidx.room3.RoomDatabase
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room3.Entity class *
-keep class com.uncaan.imit.core.database.entity.** { *; }
-keep class com.uncaan.imit.core.database.dao.** { *; }
-keepclassmembers class com.uncaan.imit.core.database.converter.** {
    public *;
}

# ==============================================================================
# WorkManager
# ==============================================================================
# Workers are instantiated dynamically by Koin WorkManagerFactory via reflection
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ==============================================================================
# Media3 / ExoPlayer (v1.11.0)
# ==============================================================================
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.extractor.** { *; }
-keep class androidx.media3.ui.** { *; }
-keep class androidx.media3.session.** { *; }
-dontwarn androidx.media3.**

# ==============================================================================
# Coil 3 (v3.4.0)
# ==============================================================================
-dontwarn coil3.**
-keep class coil3.** { *; }
-keep class * implements coil3.SingletonImageLoader$Factory { *; }
