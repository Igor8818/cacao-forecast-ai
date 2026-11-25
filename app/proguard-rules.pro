-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations

# TensorFlow Lite (Interpreter + XNNPACK)
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Room & SQLite
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.cacaoforecast.ai.data.local.** { *; }
-keep class com.cacaoforecast.ai.data.backup.** { *; }

# Modelos de domínio, ML e PDF
-keep class com.cacaoforecast.ai.domain.** { *; }
-keep class com.cacaoforecast.ai.ml.** { *; }
-keep class com.cacaoforecast.ai.pdf.** { *; }
