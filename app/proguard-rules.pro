# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Firebase Realtime Database
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class com.clinic.appointmentbooking.model.** {
    <init>();
    <fields>;
}

# Firebase Auth
-keepattributes EnclosingMethod
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
