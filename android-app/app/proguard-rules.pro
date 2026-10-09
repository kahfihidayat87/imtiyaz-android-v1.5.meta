# ============================================================================
# ProGuard/R8 rules untuk Imtiyaz Tour v2.13.0
# ============================================================================

# ---------- Umum ----------
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------- Kotlin ----------
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# ---------- Compose ----------
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.platform.** { *; }
-dontwarn androidx.compose.**

# ---------- Retrofit ----------
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepclasseswithmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# ---------- OkHttp ----------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ---------- Gson (model classes) ----------
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# Model classes app
-keep class com.imtiyaztour.app.**Model** { *; }
-keep class com.imtiyaztour.app.**Request** { *; }
-keep class com.imtiyaztour.app.**Response** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ---------- Firebase / Firestore ----------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.Exclude <fields>;
    @com.google.firebase.firestore.DocumentId <fields>;
}

# ---------- LiveKit (WebRTC - JNI) ----------
-keep class io.livekit.** { *; }
-keep class org.webrtc.** { *; }
-dontwarn io.livekit.**
-dontwarn org.webrtc.**
-keepclasseswithmembernames class * {
    native <methods>;
}

# ---------- osmdroid (OSM maps) ----------
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# ---------- Google Play Services Location ----------
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.location.**

# ---------- Kotlinx Coroutines ----------
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ---------- Enums (jangan di-obfuscate, sering dipakai reflection) ----------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------- Parcelable ----------
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ---------- Serializable ----------
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ---------- R8 default optimizations ----------
-dontoptimize
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# ---------- Suppress warning umum ----------
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.**
-dontwarn java.nio.file.**
-dontwarn org.codehaus.mojo.**