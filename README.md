# TripKitUI for Android

## Set up TripKitUI

### Add TripKitUI to your Android project

First, add [JitPack maven](https://jitpack.io/):

```groovy
allprojects {
  repositories {
    jcenter()
    maven { url "https://jitpack.io" }
  }
}
```

Add `android.enableJetifier=true` on your `gradle.properties` file

```groovy
android.useAndroidX=true
....
android.enableJetifier=true
```

Then, in app's build file, add `TripKitAndroidUI` dependency into dependencies section:

```groovy
dependencies {
  implementation 'com.github.skedgo:tripkit-android-ui:<insert-newest-version-here>'
}
```
[![Release](https://jitpack.io/v/skedgo/tripkit-android-ui.svg)](https://jitpack.io/#skedgo/tripkit-android-ui)

For a full setup, you can have a look at TripKitUISample' build file [here](https://github.com/skedgo/tripkit-android-ui/blob/master/tripkituisample/build.gradle).

#### Required configuration

##### Supported Android versions

TripKitUI supports for Android apps running [Android 4.0.3](https://developer.android.com/about/versions/android-4.0.3.html) and above. To make sure that it works in your Android app, please specify `minSdkVersion` in your `build.gradle` file to `15`:

```groovy
android {
  defaultConfig {
    minSdkVersion 16
  }
}
```

##### Get an API key

An API key is necessary to use TripKit's services, such as A-2-B routing, and all-day routing. In order to obtain an API key, you can sign up at [https://tripgo.3scale.net](https://tripgo.3scale.net/).

#### Create TripKitUI instance to access both TripKit and TripKitUI services

We recommend to have an `Application` subclass. Next, in the `onCreate()` method, you can initiate following setup:

for `>= v2.1.43`
```kotlin
class App : Application() {
  override fun onCreate() {
    super.onCreate()
     
    val baseConfig = TripKitUI.buildTripKitConfig(applicationContext, Key.ApiKey("YOUR_API_KEY"))
    val httpClientModule = HttpClientModule(null, BuildConfig.VERSION_NAME, baseConfig, getSharedPreferences("data_pref_name", MODE_PRIVATE))

    val appConfigs = TripKitConfigs.builder().from(baseConfig).build()
    TripKitUI.initialize(this, Key.ApiKey("YOUR_API_KEY"), appConfigs, httpClientModule)       
  }
}

```

With `"YOUR_API_KEY"` is the key that you obtained from [https://tripgo.3scale.net](https://tripgo.3scale.net) in the previous step.
