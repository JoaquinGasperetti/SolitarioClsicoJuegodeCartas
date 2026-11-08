# Add project-specific ProGuard rules here.

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Moshi
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Retrofit
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Coroutines
-keepattributes EnclosingMethod,InnerClasses,Signature
-keepclassmembers class kotlin.coroutines.jvm.internal.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Keep the launcher Activity in minified Release APKs.
-keep class com.HKemtrentainment.solitaro.MainActivity { *; }
