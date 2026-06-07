# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK tools proguard/proguard-android.txt

# Keep the core model classes for Gson serialization
-keep class com.voibiz.lanchat.core.model.** { *; }
-keep class com.voibiz.lanchat.core.protocol.** { *; }
