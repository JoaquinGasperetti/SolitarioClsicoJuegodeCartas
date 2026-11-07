# Add project specific ProGuard rules here.
# You can use the set property includeActionBar to override default settings.
# For details see:
#   http://developer.android.com/guide/developing/tools/proguard.html

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
