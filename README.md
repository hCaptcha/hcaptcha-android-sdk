## Android SDK for hCaptcha

![CI](https://github.com/hCaptcha/hcaptcha-android-sdk/workflows/Android%20SDK%20CI/badge.svg)
[![Release](https://jitpack.io/v/hCaptcha/hcaptcha-android-sdk.svg)](https://jitpack.io/#hCaptcha/hcaptcha-android-sdk)

###### [Installation](#installation) | [Basic usage](#basic-usage) | [Example](#display-a-hcaptcha-challenge) | [Customization](#config-params) | [Error handling](#error-handling) | [Testing](#testing) | [Publishing](#publishing)

This SDK provides a wrapper for [hCaptcha](https://www.hcaptcha.com), and is a drop-in replacement for the SafetyNet reCAPTCHA API. You will need to configure a `site key` and a `secret key` from your hCaptcha account in order to use it.


### Installation

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

*Note: replace `x.y.z` with one from [Release](https://github.com/hCaptcha/hcaptcha-android-sdk/releases) (e.g. `1.0.0`).*

### Example app

The current repository comes with an example Android application demonstrating 3 different hCaptcha usage patterns. 

See the code example below along with the possible customization to enable human verification in your Android application.

### Basic usage


<img src="/assets/hcaptcha-invisible-example.gif" alt="invisible hcaptcha example" width="300px"/>


#### Display a hCaptcha challenge

The following snippet code will ask the user to complete a challenge. 

```java
import com.hcaptcha.sdk.*;
import com.hcaptcha.sdk.tasks.*;

HCaptcha.getClient(this).verifyWithHCaptcha(YOUR_API_SITE_KEY)
    .addOnSuccessListener(new OnSuccessListener<HCaptchaTokenResponse>() {
        @Override
        public void onSuccess(HCaptchaTokenResponse response) {
            String userResponseToken = response.getTokenResult();
            // Validate the user response token using the hCAPTCHA siteverify API.
        }
    })
    .addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(HCaptchaException e) {
            Log.d("MainActivity", "Error code: " + e.getStatusCode());
            Log.d("MainActivity", "Error msg: " + e.getMessage());
        }
    });
```

You can also customize the look and feel, language, endpoint, etc. by passing a `HCaptchaConfig` object to `verifyWithHCaptcha` method.

```java
final HCaptchaConfig config = HCaptchaConfig.builder()
                .siteKey(YOUR_API_SITE_KEY)
                .apiEndpoint("https://js.hcaptcha.com/1/api.js")
                .locale("ro")
                .size(HCaptchaSize.INVISIBLE)
                .loading(false)
                .theme(HCaptchaTheme.DARK)
                .build();
HCaptcha.getClient(this).verifyWithHCaptcha(config)...;
```

##### Config params


|Name|Values/Type|Required|Default|Description|
|---|---|---|---|---|
|`siteKey`|String|**Yes**|-|This is your sitekey, this allows you to load challenges. If you need a sitekey, please visit [hCaptcha](https://www.hcaptcha.com), and sign up to get your sitekey.|
|`size`|Enum|No|INVISIBLE|This specifies the "size" of the checkbox component. By default, the checkbox is invisible and the challenge is shown automatically.|
|`theme`|Enum|No|LIGHT|hCaptcha supports light, dark, and contrast themes.|
|`locale`|String (ISO 639-2 code)|No|AUTO|You can enforce a specific language or let hCaptcha auto-detect the local language based on user's device.|
|`sentry`|Boolean|No|True|See Enterprise docs.|
|`rqdata`|String|No|-|See Enterprise docs.|
|`apiEndpoint`|String|No|-|See Enterprise docs.|
|`endpoint`|String|No|-|See Enterprise docs.|
|`reportapi`|String|No|-|See Enterprise docs.|
|`assethost`|String|No|-|See Enterprise docs.|
|`imghost`|String|No|-|See Enterprise docs.|
|`customTheme`|Stringified JSON|No|-|See Enterprise docs.|
|`host`|String|No|-|See Enterprise docs.|
|`loading`|Boolean|No|True|Show or hide the loading dialog.|

#### Error handling

In some scenarios in which the human verification process cannot be completed, you should add logic to gracefully handle the errors. The following is a list of possible error codes:

* NETWORK_ERROR (7): there is no internet connection
* SESSION_TIMEOUT (15): the challenge expired
* CHALLENGE_CLOSED (30): the challenge was closed by the user
* RATE_LIMITED (31): spam detected
* ERROR (29): general failure


#### Verify the completed challenge

After retrieving a `token`, you should pass it to your backend in order to verify the validity of the token by doing a [server side check](https://docs.hcaptcha.com/#server) using the hCaptcha secret linked to your sitekey.

---

### For maintainers

To see available gradle tasks run: `gradlew tasks`.

#### Testing
There is automated testing for every `push` command through github actions (see `.github/workflows/ci.yml`).

You can manually test before pushing by running both unit tests and instrumented tests:
* ```gradlew test```
* ```gradlew connectedDebugAndroidTest```

#### Publishing
To publish a new version follow the next steps:
1. Bump versions in the `sdk/build.gradle` file:
   * `android.defaultConfig.versionCode`: increment by **1** (next integer)
   * `android.defaultConfig.versionName`: [Semantic Versioning](https://semver.org)
2. Create a [Github Release](https://docs.github.com/en/free-pro-team@latest/github/administering-a-repository/managing-releases-in-a-repository#creating-a-release) with the **SAME** version from step 1 (**without** a prefix such as `v`)
   * JitPack's automatic process will be triggered upon first installation of the new package version
