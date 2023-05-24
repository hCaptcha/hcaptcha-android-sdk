-dontwarn javax.annotation.processing.**
-dontwarn lombok.core.configuration.**
-dontwarn org.apache.tools.**

# Prevent obfuscating the names when serializing to JSON
-keep class com.hcaptcha.sdk.HCaptchaConfig { *; }

-keepclasseswithmembernames public enum com.hcaptcha.sdk.** { *; }
