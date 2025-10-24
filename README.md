# Android SDK for hCaptcha

![CI](https://github.com/hCaptcha/hcaptcha-android-sdk/workflows/ci/badge.svg)
[![Release](https://jitpack.io/v/hcaptcha/hcaptcha-android-sdk.svg)](https://jitpack.io/#hcaptcha/hcaptcha-android-sdk)
[![Minimal Android OS](https://img.shields.io/badge/Android%20OS%20-%3E=4.1-blue.svg)](https://developer.android.com/about/dashboards)

###### [Installation](#installation) | [Requirements](#requirements) | [Example App](#example-app) | [Usage](#usage) | [Customization](#config-params) | [Error Handling](#error-handling) | [Debugging](#debugging-tips) | [FAQ](#faq)

This SDK provides a wrapper for [hCaptcha](https://www.hcaptcha.com). It is a drop-in replacement for the SafetyNet reCAPTCHA API. You will need to configure a `site key` and a `secret key` from your hCaptcha account in order to use it.


## Installation

<pre>
// Register JitPack Repository inside the root build.gradle file
repositories {
    <b>maven { url 'https://jitpack.io' }</b> 
}
// Add hCaptcha sdk dependency inside the app's build.gradle file
dependencies {
    // For Android View
    <b>implementation 'com.github.hCaptcha.hcaptcha-android-sdk:sdk:x.y.z'</b>
    // For Jetpack Compose
    <b>implementation 'com.github.hCaptcha.hcaptcha-android-sdk:compose-sdk:x.y.z'</b>
}
</pre>

*Note: replace `x.y.z` with one from [Release](https://github.com/hCaptcha/hcaptcha-android-sdk/releases) (e.g. `1.0.0`).*

### Legacy (versions < 5.0)

<pre>
// Register JitPack Repository inside the root build.gradle file
repositories {
    <b>maven { url 'https://jitpack.io' }</b> 
}
// Add hCaptcha sdk dependency inside the app's build.gradle file
dependencies {
    <b>implementation 'com.github.hcaptcha:hcaptcha-android-sdk:x.y.z'</b>
}
</pre>

## Requirements

| Platform   | Requirements                                 |
|------------|----------------------------------------------|
| Android OS | :white_check_mark: >= 4.1 (Android API 16)   |
| Wear OS    | :heavy_multiplication_x:                     |

## Example App

The current repository comes with an example Android application demonstrating 3 different hCaptcha usage patterns. 

See the code example below along with the possible customization to enable human verification in your Android application.

<img src="/assets/hcaptcha-invisible-example.gif" alt="invisible hcaptcha example" width="300px"/>

## Usage

There are multiple ways to run a hCaptcha human verification. See the below snippet for the overall flow.

```java
import com.hcaptcha.sdk.*;
import com.hcaptcha.sdk.tasks.*;

// =================================================
// 1. Initialize a client using the current activity
final HCaptcha hCaptcha = HCaptcha.getClient(this);

// =================================================
// 2. Add the desired listeners
hCaptcha
  .addOnSuccessListener(new OnSuccessListener<HCaptchaTokenResponse>() {
    @Override
    public void onSuccess(HCaptchaTokenResponse response) {
        // Successul verification. The resulting token must be passed to your backend to be validated.
        String userResponseToken = response.getTokenResult();
        Log.d("hCaptcha", "hCaptcha success. Token: " + userResponseToken");
    }
  })
  .addOnFailureListener(new OnFailureListener() {
    @Override
    public void onFailure(HCaptchaException e) {
        // Error handling here: trigger another verification, display a toast, etc.
        Log.d("hCaptcha", "hCaptcha failed: " + e.getMessage() + "(" + e.getStatusCode() + ")");
    }
  })
  .addOnOpenListener(new OnOpenListener() {
    @Override
    public void onOpen() {
        // Usefull for analytics purposes
        Log.d("hCaptcha", "hCaptcha is now visible.");
    }
  });

// =================================================
// 3. Trigger the verification process which may or may not require user input. 
//    It depends on the sitekey difficulty setting and the hCaptcha client configuration.
// 3.1 Optionaly, setup the client to pre-warm the assets.
//     It helps speeding up the actual verification process by having the assets locally already.
hCaptcha.setup(/* args */);
// 3.2 Invoke the actual verification process
//     If "setup(/* args */)" was used, this should be called with empty args.
hCaptcha.verifyWithHCaptcha(/* args */).

// The "args" for setup and verifyWithHCaptcha can be the following:
// 1. The sitekey string.
final String SITE_KEY = "10000000-ffff-ffff-ffff-000000000001";
hCaptcha.setup(SITE_KEY).verifyWithHCaptcha()
// 2. An "HCaptchaConfig" object which allows customization 
//    of the look and feel, the language and more. 
//    See section "Config Params" below.
final HCaptchaConfig hCaptchaConfig = HCaptchaConfig.builder()
        .siteKey("10000000-ffff-ffff-ffff-000000000001")
        .size(HCaptchaSize.NORMAL)
        .theme(HCaptchaTheme.LIGHT)
        .build();
hCaptcha.setup(hCaptchaConfig).verifyWithHCaptcha()
// 3. No params. Sitekey must be configured via `AndroidManifest.xml`.
hCaptcha.setup().verifyWithHCaptcha()
// Set sitekey in AndroidManifest.xml (required only for option 3)
<?xml version="1.0" encoding="utf-8"?>
<application ...>
   <meta-data android:name="com.hcaptcha.sdk.site-key"
              android:value="YOUR_API_SITE_KEY" />
</application>
</manifest>
```

To remove a specific listener you may use `HCaptcha.removeOn[Success|Failure|Open]Listener(listener)`.

To remove all listeners you may use `HCaptcha.removeAllListener()`.

Note ⚠️: For any sitekey that can show visual challenges, `HCaptcha.getClient(Activity)` must be called with `FragmentActivity` instance.

`Activity` is allowed only when `hideDialog=true` and sitekey setting is Passive (Enterprise feature).


```java
...
OnSuccessListener<HCaptchaTokenResponse> firstListener = new OnSuccessListener<HCaptchaTokenResponse>() {
    @Override
    public void onSuccess(HCaptchaTokenResponse response) {
        ...
    }
};
hCaptcha.addOnSuccessListener(firstListener).verifyWithHCaptcha();
...
OnSuccessListener<HCaptchaTokenResponse> secondListener = new OnSuccessListener<HCaptchaTokenResponse>() {
    @Override
    public void onSuccess(HCaptchaTokenResponse response) {
        ...
    }
};
hCaptcha.removeOnSuccessListener(firstListener)
    .addOnSuccessListener(secondListener)
    .verifyWithHCaptcha();
```

### MFA Phone Support

The SDK supports phone prefix and phone number parameters for MFA (Multi-Factor Authentication) flows. You can pass these parameters using `HCaptchaVerifyParams`:

```java
// Using phone prefix (country code without '+')
HCaptchaVerifyParams verifyParams = HCaptchaVerifyParams.builder()
    .phonePrefix("44")
    .build();
hCaptcha.verifyWithHCaptcha(verifyParams);

// Using phone number (full E.164 format)
HCaptchaVerifyParams verifyParams = HCaptchaVerifyParams.builder()
    .phoneNumber("+44123456789")
    .build();
hCaptcha.verifyWithHCaptcha(verifyParams);

// Using phonePrefix with rqdata
HCaptchaVerifyParams verifyParams = HCaptchaVerifyParams.builder()
    .phonePrefix("44")
    .rqdata("custom-verification-data")
    .build();
hCaptcha.verifyWithHCaptcha(verifyParams);
```

You should use only one of `phonePrefix` or `phoneNumber`. If you pass both, `phoneNumber` will be used and `phonePrefix` will be ignored.

**Note**: The `rqdata` parameter has been moved from `HCaptchaConfig` to `HCaptchaVerifyParams` for better API consistency. The old `rqdata` property in `HCaptchaConfig` is now deprecated, and will be removed in a future major version.

**Note**: If you update verify parameters and call `validate` a second time, call `reset()` before the second validation to ensure updated parameters are recognized by hCaptcha.

### For Jetpack Compose

```kotlin
import com.hcaptcha.sdk.HCaptcha
import com.hcaptcha.sdk.HCaptchaException
import com.hcaptcha.sdk.HCaptchaTokenResponse

class HCaptchaActivity : AppCompatActivity() {

    private val hCaptcha = HCaptcha.getClient(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hCaptcha.setup(BuildConfig.SITE_KEY).verifyWithHCaptcha()
        hCaptcha.addOnSuccessListener { response: HCaptchaTokenResponse ->
            val userResponseToken = response.tokenResult
            val intent = Intent()
            intent.putExtra("captcha", userResponseToken)
            setResult(RESULT_OK, intent)
            finish()
        }.addOnFailureListener { e: HCaptchaException ->
            // Error handling here: trigger another verification, display a toast, etc.
            Log.d("hCaptcha", "hCaptcha failed: " + e.getMessage() + "(" + e.getStatusCode() + ")")
            setResult(RESULT_CANCELED)
            finish()
        }.addOnOpenListener { 
            // Usefull for analytics purposes
            Log.d("hCaptcha", "hCaptcha is now visible.")
        }
    }
}
```
To fetch token data from activity in @Composable Class: 

```kotlin
val intent = Intent(context, HCaptchaActivity::class.java)
                val launcher =
                    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                        val data: Intent? = result.data
                        when (result.resultCode) {
                            Activity.RESULT_OK -> {
                                data?.let {
                                    captcha.value = data.extras?.getString("captcha")?: ""
                                }                        
                            }
                            Activity.RESULT_CANCELED -> {
                                Log.d("hCaptcha", "hCaptcha failed")
                            }
                        }
                    }
                SideEffect {
                    launcher.launch(intent)
                }
```

### Memory usage (fragment lifecycle)

We cache the fragment instance inside the SDK to speed up the next HCaptcha.verifyWithHCaptcha calls.

Once you are done with verification, you can call `HCaptcha.reset()` to release all allocated resources including the strong reference to `com.hcaptcha.sdk.HCaptchaDialogFragment`.

Note: If you do not call `.reset()` you will likely see a warning from tools like LeakCanary.

### Good to know
1. The listeners (`onSuccess`, `onFailure`, `onOpen`) can be called multiple times in the following cases:
   1. the same client is used to invoke multiple verifications
   2. the config option `resetOnTimeout(true)` is used which will automatically trigger a new verification when the current token expired. This will result in a new success or error callback.
      * deprecated, please use [`HCaptchaConfig.retryPredicate`](#retry-failed-verification)
   2. the config option [`HCaptchaConfig.retryPredicate`](#retry-failed-verification) is used to automatically trigger a new verification when some error occurs. If `HCaptchaConfig.retryPredicate` returns `true`, this will result in a new success or error callback.
   3. `onFailure` with `TOKEN_TIMEOUT` will be called once the token is expired. To prevent this you can call `HCaptchaTokenResponse.markUsed` once the token is utilized. Also, you can change expiration timeout with `HCaptchaConfigBuilder.tokenExpiration(timeout)` (default 2 min.)

### leakcanary Compatibility with Old Android Versions

Debug builds of the SDK use `leakcanary`, which has incompatibility issues with very old Android versions.

If you're testing debug builds with an old Android version and notice a `ResourceNotFound` crash when tapping on some modal elements, please remove leakcanary and retest.

## Config Params

The following list contains configuration properties to allows customization of the hCaptcha verification flow.

| Name                          | Values/Type                                          | Required | Default                          | Description                                                                                                                                                          |
|-------------------------------|------------------------------------------------------|----------|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `siteKey`                     | String                                               | **Yes**  | -                                | This is your sitekey, this allows you to load challenges. If you need a sitekey, please visit [hCaptcha](https://www.hcaptcha.com), and sign up to get your sitekey. |
| `size`                        | Enum                                                 | No       | INVISIBLE                        | This specifies the "size" of the checkbox component. By default, the checkbox is invisible and the challenge is shown automatically. Note: "invisible" refers only to the *checkbox*. If you want no or few visual challenges, choose Passive or 99.9% Passive as the behavior type for the sitekey in the hCaptcha dashboard.                                |
| `orientation`                 | Enum                                                 | No       | PORTRAIT                         | This specifies the "orientation" of the challenge.                                                                                                                   |
| `theme`                       | Enum                                                 | No       | LIGHT                            | hCaptcha supports light, dark, and contrast themes.                                                                                                                  |
| `locale`                      | String (ISO 639-1 code)                              | No       | AUTO                             | You can enforce a specific language or let hCaptcha auto-detect the local language based on user's device.                                                           |
| `resetOnTimeout`              | Boolean                                              | No       | False                            | (DEPRECATED, use `retryPredicate`) Automatically reload to fetch new challenge if user does not submit challenge. (Matches iOS SDK behavior.)                        |
| `retryPredicate`              | Lambda<sup>[*](#retry-predicate-serialization)</sup> | No       | -                                | Automatically trigger a new verification when some error occurs.                                                                                                     |
| `jsSrc`                       | String (URL)                                         | No       | https://js.hcaptcha.com/1/api.js | See Enterprise docs.                                                                                                                                                 |
| `sentry`                      | Boolean                                              | No       | True                             | See Enterprise docs.                                                                                                                                                 |
| `rqdata`                      | String                                               | No       | -                                | **Deprecated**: Use `rqdata` in `HCaptchaVerifyParams` instead. Will be removed in future releases.                                                               |
| `apiEndpoint`                 | String (URL)                                         | No       | -                                | (DEPRECATED, use `jsSrc`) See Enterprise docs.                                                                                                                       |
| `endpoint`                    | String (URL)                                         | No       | -                                | See Enterprise docs.                                                                                                                                                 |
| `reportapi`                   | String (URL)                                         | No       | -                                | See Enterprise docs.                                                                                                                                                 |
| `assethost`                   | String (URL)                                         | No       | -                                | See Enterprise docs.                                                                                                                                                 |
| `imghost`                     | String (URL)                                         | No       | -                                | See Enterprise docs.                                                                                                                                                 |
| `customTheme`                 | Stringified JSON                                     | No       | -                                | See Enterprise docs.                                                                                                                                                 |
| `host`                        | String (hostname)                                    | No       | -                                | See Enterprise docs. Do not include scheme; remove `http(s)://`.                                                                                                     |
| `loading`                     | Boolean                                              | No       | True                             | Show or hide the loading dialog.                                                                                                                                     |
| `hideDialog`                  | Boolean                                              | No       | False                            | To be used in combination with a passive sitekey when no user interaction is required. See Enterprise docs.                                                          |
| `tokenExpiration`             | long                                                 | No       | 120                              | hCaptcha token expiration timeout (seconds).                                                                                                                         |
| `diagnosticLog`               | Boolean                                              | No       | False                            | Emit detailed console logs for debugging                                                                                                                             |
| `disableHardwareAcceleration` | Boolean                                              | No       | True                             | Disable WebView hardware acceleration                                                                                                                                |

## Verify Params

The following list contains parameters for `HCaptchaVerifyParams` used during verification calls.

| Name                          | Values/Type                                          | Required | Default                          | Description                                                                                                                                                          |
|-------------------------------|------------------------------------------------------|----------|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `phonePrefix`                 | String                                               | No       | -                                | Optional phone country calling code (without '+'), e.g., "44". Used in MFA flows.                                                                                    |
| `phoneNumber`                 | String                                               | No       | -                                | Optional full phone number in E.164 format ("+44123..."), for use in MFA.                                                                                            |
| `rqdata`                      | String                                               | No       | -                                | Custom supplied challenge data. See Enterprise docs.                                                                                                                 |

### Config Examples

1. Ask the user to complete a challenge without requiring a previous checkbox tap.

```java
final HCaptchaConfig config = HCaptchaConfig.builder()
        .siteKey(YOUR_API_SITE_KEY)
        .size(HCaptchaSize.INVISIBLE)
        .build();
```

2. Set a specific language, use a dark theme and a compact checkbox.
```java
final HCaptchaConfig config = HCaptchaConfig.builder()
                .siteKey("YOUR_API_SITE_KEY")
                .locale("ro")
                .size(HCaptchaSize.COMPACT)
                .theme(HCaptchaTheme.DARK)
                .build();
```


## Error handling

In some scenarios in which the human verification process cannot be completed. 
You can add logic to gracefully handle the errors. 

The following is a list of possible error codes:

| Name                           | Code | Description                                        |
|--------------------------------|------|----------------------------------------------------|
| `NETWORK_ERROR`                | 7    | There is no internet connection.                   |
| `INVALID_DATA`                 | 8    | Invalid data is not accepted by endpoints.         |
| `CHALLENGE_ERROR`              | 9    | JS client encountered an error on challenge setup. |
| `INTERNAL_ERROR`               | 10   | JS client encountered an internal error.           |
| `SESSION_TIMEOUT`              | 15   | The challenge expired.                             |
| `TOKEN_TIMEOUT`                | 16   | The token expired.                                 |
| `CHALLENGE_CLOSED`             | 30   | The challenge was closed by the user.              |
| `RATE_LIMITED`                 | 31   | Spam detected.                                     |
| `INVALID_CUSTOM_THEME`         | 32   | Invalid custom theme.                              |
| `INSECURE_HTTP_REQUEST_ERROR`  | 33   | Insecure resource requested.                       |
| `ERROR`                        | 29   | General failure.                                   |

### Retry Failed Verification

You can indicate an automatic verification retry by setting the lambda config `HCaptchaConfig.retryPredicate`.

One must be careful to not introduce infinite retries and thus blocking the user from error recovering.

Example below will automatically retry in case of `CHALLENGE_CLOSED` error:
```java
final HCaptchaConfig config = HCaptchaConfig.builder()
        .siteKey("YOUR_API_SITE_KEY")
        .retryPredicate((config, hCaptchaException) -> {
            return hCaptchaException.getHCaptchaError() == HCaptchaError.CHALLENGE_CLOSED;
        })
        .build();
...
```

### Retry predicate serialization

Lambda may implicitly capture variables from its surrounding context.
For a lambda to be serializable, all the captured variables must be serializable as well.
Failing to meet this requirement can result in runtime errors when attempting to deserialize the lambda.

The `retryPredicate` is part of `HCaptchaConfig` that may get persist during application lifecycle.
So pay attention to this aspect and make sure that `retryPredicate` is serializable to avoid
`android.os.BadParcelableException` in run-time.

## Debugging Tips

Useful error messages are often rendered on the hCaptcha checkbox. For example, if the sitekey within your config is invalid, you'll see a message there. To quickly debug your local instance using this tool, set `.size(HCaptchaSize.NORMAL)`

`HCaptchaConfigBuilder.diagnosticLog(true)` can help to get more detailed logs.

### Verify the completed challenge

After retrieving a `token`, you should pass it to your backend in order to verify the validity of the token by doing a [server side check](https://docs.hcaptcha.com/#server) using the hCaptcha secret linked to your sitekey.

---

## FAQ

> Can I get a token in a non-UI thread?

No: the SDK depends on WebView, which is a UI component and cannot be instantiated in a non-UI thread.

However, the SDK provides a completely silent (invisible to the end-user) mechanism with `hideDialog=true` config + "passive" site key (this is an Enterprise feature). But note that the token request still has to be called from the UI thread.

> How can I prevent the hCaptcha verification from being canceled when the back button is pressed?

It is possible by specifying `HCaptchaConfig.retryPredicate` as shown in the following code snippet:

```java
final HCaptchaConfig config = HCaptchaConfig.builder()
        .siteKey("YOUR_API_SITE_KEY")
        .retryPredicate((config, hCaptchaException) -> {
            return hCaptchaException.getHCaptchaError() == HCaptchaError.CHALLENGE_CLOSED;
        })
        .build();
```

> HCaptcha constantly failing with IllegalStateException "Visual Challenge verification require FragmentActivity", how to fix it?

SDK expect to be initialized with `FragmentActivity` instance in regular scenario.

In case if you use passive `siteKey` make sure that you called `hideDialog(true)` on `HCaptchaCconfig.builder()`

> I'm getting build conflicts with `android:usesCleartextTraffic` attribute. What should I do?

The SDK sets `android:usesCleartextTraffic="false"` in the application manifest to ensure secure network communication by default. If your app already has another library that sets this attribute to `true`, you may encounter build conflicts.

**Solution:** It's safe to override this value to `true` if your app specifically requires cleartext traffic. However, cleartext traffic is not secure and should only be enabled if absolutely necessary. Consider using HTTPS for all network communications instead.

For more information about this attribute and security implications, see the [Android Developer Documentation](https://developer.android.com/guide/topics/manifest/application-element#usesCleartextTraffic).

## For maintainers

If you plan to contribute to the repo, please see [MAINTAINERS.md](./MAINTAINERS.md) for detailed build, test, and release instructions.

## Implementation Notes

### Memory Lifecycle Management

Some resources are cached to speed up subsequent calls. Once you call `HCaptcha.reset()` it releases these resources.

### Retry handling

`retryPredicate` is the only mechanism used to auto-retry and the default only honors `resetOnTimeout` for SESSION_TIMEOUT (15). It does not retry NETWORK_ERROR (7), so add your own predicate if you want that behavior (be sure to bound retries/backoff).

### FragmentActivity requirement

Visual challenges throw if you pass a non-FragmentActivity. A plain Activity is only supported for headless mode (hideDialog=true) with a Passive sitekey.

### Listener management

Listeners persist across verifications and are not auto-removed. If you won’t reuse the same HCaptcha instance or want to avoid duplicate callbacks/leaks, remove them in teardown: `removeOn...Listener` or `removeAllListeners()`, and call `HCaptcha.reset()` when done to release the dialog/webview.

### Configuration changes

The dialog is a DialogFragment; Android will recreate it on configuration changes. The SDK will re-render the challenge, but it does not live‑update theme/locale mid-session. To apply new locale or light/dark mode, recreate with an updated HCaptchaConfig and re‑verify. If you need to preserve UX across changes, handle it in the host, e.g. restart verification with updated config, or constrain orientation while a challenge is active.

### HCaptcha.reset() usage

This tears down the current verifier and releases UI resources. It dismisses the dialog if shown, resets/destroys the WebView, clears the cached/preloaded WebView, and drops the internal verifier reference.


#### When to call

Call on Activity teardown if you won't reuse the same HCaptcha instance. This avoids leaks from the cached fragment/WebView.

If you expect multiple verifications in the same screen, don't call reset() between them: the SDK caches the WebView/fragment to speed up subsequent calls.

### Before initializing

This is safe but usually unnecessary. `setup()` / `verifyWithHCaptcha()` already re-initializes as needed, so calling reset() first just forces a fresh verifier. If you're creating a brand‑new HCaptcha instance, calling reset() on it is a no‑op.

#### Cleanup

- Listeners persist; reset() does not remove them. If your listeners capture an Activity/Fragment, remove them during teardown to avoid leaks: removeOn...Listener(...) or removeAllListeners()

- After a successful solve, the SDK schedules a token timeout callback. Call `HCaptchaTokenResponse.markUsed()` once you've used the token to cancel that timer and prevent a later TOKEN_TIMEOUT callback
