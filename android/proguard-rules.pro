# Optimized Proguard Rules for English Tracking AI
# Target: 40-50%+ Optimization on Play Store

# 1. Keep our Data Models (Serialization/Firebase/GSON)
# We keep these because their field names are used for mapping JSON/Firestore data.
-keep class com.smnm.englishtrackingai.VocabWord { *; }
-keep class com.smnm.englishtrackingai.VocabData { *; }
-keep class com.smnm.englishtrackingai.ChatMessage { *; }
-keep class com.smnm.englishtrackingai.VocabData$* { *; }

# 2. WebRTC (Critical for calls)
# Native components require full retention.
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# 3. Socket.io & OkHttp (Surgical Keeps)
# Instead of keeping everything, we only keep what's needed for reflection and connectivity.
-keepnames class io.socket.** { *; }
-keepclassmembers class io.socket.client.Socket {
    public *** *(...);
}
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# 4. GSON Rules
# Gson uses reflection to serialize/deserialize data.
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# 5. Firebase & Play Services
# We remove the broad -keep rule. Firebase libraries include their own 'consumer ProGuard rules'
# which handle themselves. We only keep specific things if crashes occur.
-dontwarn com.google.android.gms.**
-dontwarn com.google.firebase.**

# 6. General Optimizations
-dontwarn java.lang.invoke.**
-dontwarn **.R$*

# 7. WorkManager & Room (Fixes WorkDatabase crash)
# R8 Full Mode can be too aggressive with Room/WorkManager reflection.
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.room.MultiInstanceInvalidationService { *; }
-keep class androidx.room.paging.LimitOffsetPagingSource { *; }
-keep class androidx.room.RoomDatabase { *; }
-dontwarn androidx.work.impl.WorkDatabase_Impl
-dontwarn androidx.room.paging.LimitOffsetPagingSource
