# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Keep Room database entities
-keep class com.calorietracker.database.** { *; }

# Keep API models for JSON serialization (Gson uses reflection on these classes)
-keep class com.calorietracker.api.** { *; }
-keepclassmembers class com.calorietracker.api.** {
    <fields>;
}

# Keep Health Connect integration
-keep class androidx.health.connect.client.** { *; }

# Keep ZXing for QR code scanning
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }

# Keep Gson/Retrofit models
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class retrofit2.** { *; }

# Keep security-related classes
-keep class com.calorietracker.security.** { *; }

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Keep app components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}