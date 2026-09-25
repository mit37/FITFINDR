# Add project specific ProGuard rules here.
# Keep MediaPipe / LiteRT-LM and kotlinx.serialization models from being
# stripped or renamed in ways that break reflection-based (de)serialization.
-keep class com.google.mediapipe.** { *; }
-keepattributes *Annotation*
-keepclassmembers class com.mitanshm.fitfindr.domain.** {
    *;
}
