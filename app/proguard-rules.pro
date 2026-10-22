# kotlinx.serialization: @Serializable 클래스의 serializer를 유지
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.geupsik.alimi.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit 인터페이스의 제네릭 반환형 정보 유지
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
