# TripKitUI for Android

TripKitUI provides reusable Android maps, location search, trip planning, results,
trip details, timetables, and a configurable home controller, on top of TripKit core.

## Published SDK versus current source

The latest verified public release is **v2.2.1.6 (16 November 2023)**. The
`develop` branch contains newer, unreleased code. Building this checkout does not
update the published SDK. In particular, the legacy local Maven version in this
checkout is not a new public release number.

## Add the published SDK

Configure repositories in your project's `settings.gradle` (or its existing
repository configuration):

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
        maven { url 'https://repository.liferay.com/nexus/content/repositories/public/' }
    }
}
```

Select the UI module from the multi-module JitPack publication:

```groovy
dependencies {
    implementation 'com.github.skedgo.tripkit-android-ui:TripKitAndroidUI:v2.2.1.6'
}
```

The UI module exposes its core SDK dependency; do not add a different core SDK
release alongside it. The older aggregate coordinate
`com.github.skedgo:tripkit-android-ui:v2.2.1.6` includes multiple modules.
Neither coordinate represents the unreleased `develop` checkout.

Enable AndroidX and Jetifier in `gradle.properties`:

```properties
android.useAndroidX=true
android.enableJetifier=true
android.nonTransitiveRClass=false
```

The audited binary consumer uses transitive R classes (`android.nonTransitiveRClass=false`),
matching this SDK resource configuration. Consumer defaults with non-transitive R
classes have not been certified by this audit.

## Building and integrating current source

The current source requires **minSdk 23**, Java 17, Kotlin 2.0.21, Gradle 8.11.1,
and compileSdk 36. Its sample targets API 36 and uses AGP 8.9.1. AGP 8.9 is
only officially tested through API 35; successful compilation is not Android 16
certification. These requirements describe this checkout, not the 2023 binaries.
Enable core library desugaring and Data Binding in consumer applications:

```groovy
android {
    compileSdk 36
    defaultConfig { minSdk 23 }
    buildFeatures { dataBinding true }
    compileOptions {
        coreLibraryDesugaringEnabled true
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}
dependencies {
    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:2.0.4'
}
```

### Initialize current source

Initialize the SDK in your `Application.onCreate()` before creating SDK views.
Obtain a TripGo API key from [SkedGo](https://tripgo.3scale.net/). Read the key from
your own configuration; do not commit credentials.

```kotlin
val key = Key.ApiKey(yourTripGoApiKey)
val config = TripKitUI.buildTripKitConfig(applicationContext, key)
val httpClientModule = HttpClientModule(null, BuildConfig.VERSION_NAME, config)
TripKitUI.initialize(this, key, config, httpClientModule, yourGooglePlacesKey)
```

The current `HttpClientModule` fourth parameter is an optional application
activation callback. It is no longer a `SharedPreferences` parameter. Current
`Location` is `com.skedgo.tripkit.common.model.location.Location` and its `id` is
a `String`; older consumer source must be migrated accordingly.

Supply the Google Maps SDK key through manifest metadata named
`com.google.android.geo.API_KEY`. Enable Maps SDK for Android and Places for the
keys you use, and configure package/signing restrictions for your application.
Request foreground location permissions at runtime when needed; declaring them
in the merged manifest does not grant them. Background location, notifications,
calendar and camera permissions depend on the features your app enables.
Handle system insets in your host activities when targeting Android 15/16.

### Run `tripkituisample`

The sample demonstrates region-route autocomplete, location search, and the home
controller/map with sample favorites and route/detail navigation. `MapActivity`
also exists but the main map button opens `HomeViewControllerActivity`.

In this repository's untracked `local.properties`, configure:

```properties
tripgo_api_key=<your TripGo API key>
google_map_key=<your Google Maps Android API key>
google_places_key=<your Places API key>
```

`google_places_key` defaults to `google_map_key`. Gradle properties of the same
names, or environment variables `TRIPGO_API_KEY`, `GOOGLE_MAP_KEY`, and
`GOOGLE_PLACES_KEY`, can supply overrides. Environment variables take precedence.
Placeholder defaults allow compilation but cannot demonstrate working services.

```sh
./gradlew :TripKitAndroid:assembleDebug :TripKitAndroidUI:assembleDebug :tripkituisample:assembleDebug
```

## Release validation

Before a new release, validate release AARs, generated POMs and Gradle module
metadata, a separate Maven consumer, R8, and runtime functionality. The current
checkout has unresolved native-library 16 KB compatibility and validation gaps;
a successful debug build alone is not a release signoff. JitPack uses the
repository's nested core revision; release and test both repositories together.

### Host appearance and branding

Use a Material Components DayNight theme on the host Activity and select its appearance with
AppCompat's normal night-mode APIs. The SDK does not set the application's night mode.
The public controller surfaces and Compose palette read the host's `colorSurface`,
`colorOnSurface`, `colorPrimary`, `colorOnPrimary`, `colorSecondary` and related attributes.
Existing `RouteUiStyle` and TripKit typography customization remain available.

Shared legacy XML rows, transport indicators and map styling also use the SDK's named color
resources and `values-night` variants. Override those resources in both configurations when
customizing these surfaces; keep `white`/`black` as semantic background/text aliases rather than
using them as fixed icon colors. Use `colorOnPrimary` for foregrounds on a branded button.
Explicit AppCompat light/dark selection propagates through the Activity configuration to those
resources and the map. An in-place, custom theme change without Activity recreation is not a
supported refresh mechanism for cached legacy Views.

The sample feature menu includes System, Light and Dark appearance choices. Its purple host
brand demonstrates that SDK route actions need not use TripGo's green. When recreating a host,
call the public Home `load` method again to reconnect host callbacks to the restored controller;
re-supply application-owned providers after process recreation.
