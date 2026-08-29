# MileOwl ProGuard Rules

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep all model classes
-keep class com.mileowl.tracker.data.model.** { *; }

# Keep all DAO classes
-keep interface com.mileowl.tracker.data.db.** { *; }

# Keep repository
-keep class com.mileowl.tracker.data.repository.** { *; }

# ViewModels
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class com.mileowl.tracker.ui.**ViewModel { *; }

# Navigation
-keep class androidx.navigation.** { *; }
-keep class com.mileowl.tracker.ui.navigation.** { *; }

# Kotlin Serialization (if used in Nav)
-keepattributes *Annotation*, EnclosingMethod, Signature
-keep class kotlin.reflect.jvm.internal.** { *; }

# Google Play Services
-keep class com.google.android.gms.location.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
