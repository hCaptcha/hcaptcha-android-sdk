# Changelog

# 4.1.1

- Fix: back button should cancel hCaptcha in compose-sdk

# 4.1.0

- Feat: preload WebView on `setup` call

# 4.0.5

- compose-sdk: set minSdk to 21

# 4.0.4

- Downgrade: jackson-databind to 2.13.* (#170)

# 4.0.3

- Upgrade: third-party dependencies (lombok, jackson-databind) (#167)

# 4.0.2

- Fix: passive site keys (hideDialog=true) broken for `compose-sdk`

# 4.0.1

- Feat: release of `compose-sdk`

# 4.0.0

- Feat (breaking change): accept `HCaptcha.getClient(Activity)` for passive sitekeys. (#112)

# 3.11.0

- Fix: handle null `internalConfig` in args for HCaptchaDialogFragment (#140)
- Feature: drop diagnostic logs from production code (#139)
- Fix: wrong language used in `values-be/strings.xml` (#138)
- Fix: misleading exception on missing `siteKey` (#137)
- Fix: calling `webView.loadUrl` on destroyed `WebView` (#136)

# 3.10.0

- Fix: crash on insecure HTTP request handling
- Feat: new error code `INSECURE_HTTP_REQUEST_ERROR`

# 3.9.1

- Fix: add missing ProGuard rules for enums

# 3.9.0

- Feature: add config to control WebView hardware acceleration `HCaptchaConfig.disableHardwareAcceleration`
- Fix: removed unsafe cast with improved public api

# 3.8.2

- Bugfix: handle BadParcelableException when hCaptcha fragment needs to be recreated due to app resume

# 3.8.1

- Bugfix: report error when missing WebView provider

# 3.8.0

- Feat: new `HCaptcha.reset` to force stop verification and release all resources.

# 3.7.0

- Feat: new `HCaptchaConfig.orientation` to set either `portrait` or `landscape` challenge orientation.

# 3.6.0

- Feat: new `HCaptcha.removeAllListener` and `HCaptcha.removeOn[Success|Failure|Open]Listener(listener)` to remove all or specific listener.

# 3.5.2

- Bugfix: java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState, on `verifyWithHCaptcha`

# 3.5.1

- Bugfix: Parcelable encountered IOException writing serializable object (name = com.hcaptcha.sdk.HCaptchaConfig) ([#94](https://github.com/hCaptcha/hcaptcha-android-sdk/issues/94))

# 3.5.0

- Deprecated: `HCaptchaConfig.apiEndpoint` replaced with `HCaptchaConfig.jsSrc` option

# 3.4.0

- Feat: new `HCaptchaConfig.retryPredicate` which allows conditional automatic retry
- Deprecated: `HCaptchaConfig.resetOnTimeout` replaced by more generic `HCaptchaConfig.retryPredicate` option

# 3.3.7

- Bugfix: handle Failed to load WebView provider: No WebView installed

# 3.3.6

- Bugfix: always dim background if checkbox is visible ([#72](https://github.com/hCaptcha/hcaptcha-android-sdk/issues/72))

# 3.3.5

- Show loading screen until the challenge is open when size is `HCaptchaSize.INVISIBLE`

# 3.3.4

- Rename `ic_logo` drawable to avoid possible collisions with a host app's drawables
- Prevent closing hCaptcha view on loading container click

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
