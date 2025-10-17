-dontwarn javax.annotation.processing.**
-dontwarn lombok.core.configuration.**
-dontwarn org.apache.tools.**
-dontwarn android.content.pm.PackageManager$ApplicationInfoFlags
-dontwarn edu.umd.cs.findbugs.annotations.*
-dontwarn java.beans.*
-dontwarn lombok.*

# Prevent obfuscating the names when serializing to JSON
-keep class com.hcaptcha.sdk.HCaptchaConfig { *; }
-keep class com.hcaptcha.sdk.HCaptchaVerifyParams { *; }
-keepclasseswithmembernames public enum com.hcaptcha.sdk.** {
    @com.fasterxml.jackson.annotation.JsonValue *;
}

# Remove debug logging from the production code
-assumenosideeffects class com.hcaptcha.sdk.HCaptchaLog {
    public static void *(...);
}
