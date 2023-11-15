-dontwarn javax.annotation.processing.**
-dontwarn lombok.core.configuration.**
-dontwarn org.apache.tools.**
-dontwarn android.content.pm.PackageManager$ApplicationInfoFlags
-dontwarn edu.umd.cs.findbugs.annotations.*
-dontwarn java.beans.*
-dontwarn lombok.*

# Prevent obfuscating the names when serializing to JSON
-keep class com.hcaptcha.sdk.HCaptchaConfig { *; }
-keepclasseswithmembernames public enum com.hcaptcha.sdk.** {
    @com.fasterxml.jackson.annotation.JsonValue *;
}
