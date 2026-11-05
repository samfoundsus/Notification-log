# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Room entities, DAO query models, and generated Room Database implementations
-keep class com.example.data.db.** { *; }
-keep class com.example.data.dao.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Database class * { *; }
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
-keepnames class * extends androidx.room.RoomDatabase

# Keep NotificationListenerService, WorkManager workers, and main entry points
-keep public class com.example.service.NotificationCaptureService { *; }
-keep public class com.example.MainActivity { *; }
-keep class com.example.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Paging 3 & Coroutines rules
-dontwarn androidx.paging.**
-dontwarn kotlinx.coroutines.**
