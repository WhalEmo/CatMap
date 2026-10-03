# ==========================================
# 1. TEMEL ATTRIBUTE & REFLECTION
# ==========================================
-keepattributes Signature
-keepattributes *Annotation*

# ==========================================
# 2. MODEL VE VERİ SINIFLARI
# ==========================================
-keepclassmembers class com.beem.catmap.data.model.** {
    <fields>;
    <init>(...);
    *** get*();
    *** set*(...);
    *** is*();
}

# UserModel ve Serializable sınıflar
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ==========================================
# 3. ENUM VE COMPANION NESNELERİ
# ==========================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static ** getEntries();
    <fields>;
}

-keepclassmembers class * {
    public static ** Companion;
}

# ==========================================
# 4. GÖRSEL KÜTÜPHANELERİ (COIL, GLIDE, PICASSO)
# ==========================================
# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# ==========================================
# 5. AĞ & ASYNC (OKHTTP, OKIO, COROUTINES)
# ==========================================
-keepclassmembers class * extends okhttp3.OkHttpClient { *; }
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}