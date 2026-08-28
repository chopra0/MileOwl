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

# Keep ViewModels
-keep class com.mileowl.tracker.ui.**ViewModel { *; }

# Compose
-keep class androidx.compose.material.icons.** { *; }
-keep class com.mileowl.tracker.ui.** { *; }

# Navigation
-keep class androidx.navigation.** { *; }

# Google Play Services
-keep class com.google.android.gms.location.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
