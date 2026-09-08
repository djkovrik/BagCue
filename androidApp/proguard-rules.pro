# BagCue application rules. Provider SDK consumer rules remain authoritative.
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
