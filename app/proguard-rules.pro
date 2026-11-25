# Minification is disabled on purpose (see README): keep the rules file for
# the day R8 is enabled, so the WebView/Compose integration stays untouched.

-keep class com.efx.twitterwrapper.core.update.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.efx.twitterwrapper.core.update.**$$serializer { *; }
