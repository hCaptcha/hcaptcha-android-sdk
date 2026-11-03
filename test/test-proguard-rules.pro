# Proguard rules that are applied to your test apk/code.
-keep class androidx.compose.ui.test.** { *; }
-keep class androidx.compose.ui.platform.** { *; }

# HCaptchaTest reaches the WebView by reflection to simulate its render process being gone
-keepclassmembers class com.hcaptcha.sdk.HCaptcha {
    com.hcaptcha.sdk.IHCaptchaVerifier captchaVerifier;
}
-keepclassmembers class com.hcaptcha.sdk.** {
    com.hcaptcha.sdk.HCaptchaWebViewHelper webViewHelper;
}
