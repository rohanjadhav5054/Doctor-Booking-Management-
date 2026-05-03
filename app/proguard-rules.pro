# ─────────────────────────────────────────────────────────────────────────────
# Bhagirati Hospital — ProGuard / R8 Rules
# ─────────────────────────────────────────────────────────────────────────────

# ── Required attributes (must always be kept) ─────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ══════════════════════════════════════════════════════════════════════════════
# DATA MODELS  — Firebase uses full reflection to deserialise these.
#               -keep (not just -keepclassmembers) is required to preserve
#               the class name itself, which R8 would otherwise obfuscate.
# ══════════════════════════════════════════════════════════════════════════════
-keep class com.clinic.appointmentBooking.model.** { *; }

# ══════════════════════════════════════════════════════════════════════════════
# APPLICATION LAYERS — keep all app classes so R8 cannot rename/remove them
# ══════════════════════════════════════════════════════════════════════════════
-keep class com.clinic.appointmentBooking.view.**        { *; }
-keep class com.clinic.appointmentBooking.adapter.**     { *; }
-keep class com.clinic.appointmentBooking.repository.**  { *; }
-keep class com.clinic.appointmentBooking.viewmodel.**   { *; }
-keep class com.clinic.appointmentBooking.util.**        { *; }

# Generated ViewBinding classes (e.g. ActivitySplashBinding)
-keep class com.clinic.appointmentBooking.databinding.** { *; }

# ══════════════════════════════════════════════════════════════════════════════
# FIREBASE
# ══════════════════════════════════════════════════════════════════════════════
-keep class com.google.firebase.**        { *; }
-keep class com.google.android.gms.**     { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firebase database internal serialisation helpers
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.database.PropertyName <methods>;
}

# ══════════════════════════════════════════════════════════════════════════════
# KOTLIN
# ══════════════════════════════════════════════════════════════════════════════
# Coroutines — keep dispatcher lookups used at runtime
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# Kotlin reflection (used by by viewModels() delegate, etc.)
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

# Kotlin metadata (needed by coroutines and serialisation)
-keep class kotlin.Metadata { *; }

# ══════════════════════════════════════════════════════════════════════════════
# MATERIAL COMPONENTS
# ══════════════════════════════════════════════════════════════════════════════
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ══════════════════════════════════════════════════════════════════════════════
# ENUMS  — R8 can strip enum members used only in when() expressions
# ══════════════════════════════════════════════════════════════════════════════
-keepclassmembers enum * { *; }

# ══════════════════════════════════════════════════════════════════════════════
# MISC
# ══════════════════════════════════════════════════════════════════════════════
# Suppress warnings for classes not present in the build
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
