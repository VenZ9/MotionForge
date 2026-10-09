# Proguard rules for MotionForge
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-dontwarn androidx.media3.**
