# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
-keepattributes *Annotation*

# Keep kotlinx.serialization generated serializers
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep,includedescriptorclasses class com.vakit.widget.**$$serializer { *; }
-keepclassmembers class com.vakit.widget.** {
    *** Companion;
}
-keepclasseswithmembers class com.vakit.widget.** {
    kotlinx.serialization.KSerializer serializer(...);
}
