-dontwarn javax.annotation.processing.**
-dontwarn lombok.core.configuration.**
-dontwarn org.apache.tools.**
-dontwarn android.content.pm.PackageManager$ApplicationInfoFlags
-dontwarn edu.umd.cs.findbugs.annotations.*
-dontwarn java.beans.*
-dontwarn lombok.*

# Remove debug logging from the production code
-assumenosideeffects class com.hcaptcha.sdk.HCaptchaLog {
    public static void *(...);
}
