# Keep Room entities / DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin / coroutines
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose / Navigation
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep data classes used in JSON backup
-keepclassmembers class com.luis.alhendinfc.data.local.** { *; }
-keepclassmembers class com.luis.alhendinfc.domain.model.** { *; }

# Firebase Auth + Firestore
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
