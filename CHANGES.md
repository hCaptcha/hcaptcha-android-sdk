# Changelog

# 2.3.0

- Add new boolean config option `HCaptchaConfig.fullInvisible`.
- Change the behavior of `addOnSuccessListener`, `addOnFailureListener` and `addOnOpenListener` methods. 
  - previously: the callbacks were removed after utilization
  - currently: the callbacks are persisted to be reused for future calls on the same client. This allows multiple human verifications using the same client and the same callback.  

# 2.2.0

- Add new callback `addOnOpenListener`.

## 2.1.0

- Add `HCaptcha.setup` method to improve cold-start time, enable asset caching ([#24](https://github.com/hCaptcha/hcaptcha-android-sdk/issues/24))

## 2.0.0
- Add more error codes (see readme for full list)
