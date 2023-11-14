# Proguard rules that are applied to your test apk/code.
-ignorewarnings

# -keepattributes *Annotation*
-keepattributes Signature

#-keep class androidx.fragment.app.testing.** { *; }

-assumenosideeffects class com.hcaptcha.sdk.HCaptchaDialogFragmentTest {
    *** webViewNotInstalled(...);
}

# rules to keep compiller happy with android.enableR8.fullMode=true

-keepnames class android.test.** { *; }
-keep class androidx.lifecycle.Lifecycle$Event$Companion { *; }
-keep class androidx.lifecycle.** { *; }
-keep class com.google.errorprone.annotations.MustBeClosed { *; }
-dontwarn com.sun.jna.**
