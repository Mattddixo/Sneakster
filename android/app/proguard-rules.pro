# Kotlinx Serialization ships its own consumer rules, but that's never been verified end-to-end
# here since this environment can't build/run the app module (see android/README.md). These
# small @Serializable request/response models (com.mattdixon.snake.data) are exactly what the
# backend's JSON contract depends on field-for-field, so keep them and their generated
# serializers whole rather than trust the bundled rules alone - a stripped/renamed field here
# would silently break every network call, not just bloat a stack trace.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep @kotlinx.serialization.Serializable class com.mattdixon.snake.data.** { *; }
-keep,includedescriptorclasses class com.mattdixon.snake.data.**$$serializer { *; }
-keepclassmembers class com.mattdixon.snake.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.mattdixon.snake.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
