# Changelog

# 3.3.4

- Rename `ic_logo` drawable to avoid possible collisions with a host app's drawables

# 3.3.3

- Fix Android 10 WebView crash on onCheckIsTextEditor call

# 3.3.2

- Add `HCaptchaConfig.diagnosticLog` to log diagnostics that are helpful during troubleshooting

# 3.3.1

- Fix dialog dismiss crash in specific scenario

# 3.3.0

- Disabled cleartext traffic (`android:usesCleartextTraffic="false"` added to `AndroidManifest.xml`)
- `hcaptcha-form.html` asset moved into a variable

# 3.2.0

- Add `TOKEN_TIMEOUT` error triggered after a certain configured number of seconds elapsed from the token issuance.

# 3.1.2

- Fix checkbox view not dismissible

# 3.1.1

- Fix double close error reporting

# 3.1.0

- Add `pmd`, `checkstyle`, `spotbugs` tools to build system ([#40](https://github.com/hCaptcha/hcaptcha-android-sdk/issues/40))

# 3.0.0

- Add new boolean config option `HCaptchaConfig.hideDialog`.
- (breaking change) Change the behavior of `addOnSuccessListener`, `addOnFailureListener` and `addOnOpenListener` methods. 
  - previously: the callbacks were removed after utilization
  - currently: the callbacks are persisted to be reused for future calls on the same client. This allows multiple human verifications using the same client and the same callback.  

# 2.2.0

- Add new callback `addOnOpenListener`.

## 2.1.0

- Add `HCaptcha.setup` method to improve cold-start time, enable asset caching ([#24](https://github.com/hCaptcha/hcaptcha-android-sdk/issues/24))

## 2.0.0
- Add more error codes (see readme for full list)
