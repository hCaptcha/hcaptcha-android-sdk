# Prerequisites software

- Android Studio

# Testing

There is automated testing for every `push` command through github actions (see `.github/workflows/ci.yml`).

You can manually test before pushing by running both unit tests and instrumented tests:
* `./gradlew sdk:test`
* `./gradlew test:connectedAndroidTest`
* `./gradlew test:connectedAndroidTest -P testingMinimizedBuild=true -P android.enableR8.fullMode=false`

## Manual testing

+ {normal,invisible,compact} -> verify -> success -> mark used
+ {normal,invisible,compact} -> verify -> success -> token timeout

+ {normal,invisible,compact} -> verify -> touch outside -> challenge closed
+ {normal,invisible,compact} -> verify -> back button -> challenge closed

+ {normal,invisible,compact} -> verify -> rotate device (recreate activity, i.e. without `android:configChanges` in `AndroidManifest.xml`) -> hcaptcha gone, no callbacks fired
+ {normal,invisible,compact} -> verify -> send app to background -> open app from history again -> hcaptcha is displayed

+ {hide dialog} -> verify -> token obtained -> mark used

## How to end-to-end test SDK integration before release

### When this make sense

* Proguard configuration update
* API Changes
* Breaking Changes

### How to do this

To install the SDK for a specific pull request (PR) or git branch in the same way as end-developers do, i.e. as a Gradle dependency, follow the steps below:

1. Update your dependency in `example-app/build.gradle` by replacing:
   ```groovy
   dependencies {
      // ...
      implementation project(path: ':sdk')
      // ...
   }
   ```

with:

   ```groovy
   dependencies {
      // ...
      implementation "com.github.hCaptcha.hcaptcha-android-sdk:sdk:BRANCH_NAME-SNAPSHOT"
      // or
      implementation "com.github.hCaptcha.hcaptcha-android-sdk:sdk:pull/PR_NUMBER/head-SNAPSHOT"
   }
   ```
1. Build `example-app` for `release` variant
1. Test `example-app`

> NOTE: JitPack builds dependencies on-demand, i.e. once Gradle requests the dependency.
> If the dependency for the specified PR branch has not been built yet, Gradle may fail with a timeout error.
> It can take 5-10 minutes for JitPack to build and make the dependency available to Gradle, so please be patient.

# Publishing

To publish a new version follow the next steps:

1. Bump versions in the [`sdk/build.gradle`](./sdk/build.gradle) file:
   * `android.defaultConfig.versionCode`: increment by **1** (next integer)
   * `android.defaultConfig.versionName`: [Semantic Versioning](https://semver.org)
2. Update [`CHANGES.md`](./CHANGES.md) with changes since last version
3. Create a [Github Release](https://docs.github.com/en/free-pro-team@latest/github/administering-a-repository/managing-releases-in-a-repository#creating-a-release) with the **SAME** version from step 1 (**without** a prefix such as `v`)
   * JitPack's automatic process will be triggered upon first installation of the new package version

# Known issues

### Android Studio Lombok plugin doesn't work (generated methods not found)

Install lombok plugin from https://github.com/mplushnikov/lombok-intellij-plugin/issues/1130#issuecomment-1108316265
