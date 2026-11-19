# #23656 — Public Android SDK compatibility audit

Audit date: 19 November 2026, Asia/Manila. Work is local and uncommitted. **Release readiness: NO pending the release gates below.** Build success must not be read as runtime certification.

## Baseline and publication history

| Item | Evidence-backed baseline |
|---|---|
| UI checkout | `develop`, `0e91ab9498cd02fba884fe24b7595794b1dec9e8` |
| Nested core | `develop`, `5dbec4a7cfa7e6c6b6212da779d8d294ea709c6e` |
| Existing changes | Three untracked IDE files: `.idea/AndroidProjectSystem.xml`, `.idea/kotlinc.xml`, `.idea/runConfigurations.xml`. All tracked SDK files initially clean; nested core clean. Preserved. |
| Latest UI GitHub release | `v2.2.1.6`, published 2023-11-16 15:51:17 UTC, commit `356b651f6a3c55c87e966a675dfde17bacadcddd` |
| Existing UI JitPack record | `status: ok`, same tag/commit, recorded build 2023-11-16 16:19:40.726 UTC, 15 modules. This verifies historical build metadata, not a build of current changes. |
| Latest core GitHub release | `v2.2.0`, published 2023-07-21 05:42:23 UTC; JitPack records `v2.2.0` and `2.2.0` as `ok`. |
| Core bundled in UI 2023 tag | `c9453c2517ae991ecc058df6be8be884048e4906`, distinct from standalone core release. |
| Current Maven versions | UI and core both use `com.skedgo.tripkit`, version `2.2.1.5`; stale local metadata, **not evidence of a newer public release**. `tripkitUiVersion=2` is a compatibility flag, not an artifact version. |
| Change volume | UI:974 checkout-only/2 release-only commits;1,283 changed files,+71,238/-25,565 lines at audit baseline. Core:463 checkout-only/1 release-only relative to the bundled core;524 checkout-only/11 release-only versus standalone release. History diverges; these are not simple linear ahead counts. |

Sources: [UI release](https://github.com/skedgo/tripkit-android-ui/releases/tag/v2.2.1.6), [core release](https://github.com/skedgo/tripkit-android/releases/tag/v2.2.0), existing [UI JitPack record](https://jitpack.io/api/builds/com.github.skedgo/tripkit-android-ui/v2.2.1.6), [core JitPack record](https://jitpack.io/api/builds/com.github.skedgo/tripkit-android/v2.2.0). Saved records: `evidence/jitpack-*.json`, `evidence/core-*.json`, `evidence/baseline.txt`.

No SDK-local AI Brain existed. Read inherited `AGENTS.md`, working agreement, build doctor, and Brain summary; no matching #23656 task was found in the inherited task directory. Root Brain preflight/checkpoint writes were omitted because the user restricts changes to the two SDK repositories. All records are here. Ticket access redirects to a Redmine sign-in form (`evidence/redmine-text.txt`); the existing ticket analysis could not be read without authentication. No authenticated ticket content is inferred.

The UI release-only history-thread correction and launcher exported declaration are already present in current source. No duplicate cherry-pick was needed. `evidence/release-divergence.txt` records release/checkout reachability; release preparation must compare actual patches rather than assume develop includes every tag commit.

### What was already modernized

Current build: Gradle 8.11.1, AGP 8.9.1, Kotlin 2.0.21, Java17 (installed Zulu17.0.11), compile/target SDK36, library/sample minSdk 23. The core bundled by the 2023 UI release used AGP 7.3.0, Kotlin 1.6.20, compile/target SDK 33 and minSdk 21. Current source already has AndroidX, namespaces, Parcelize, Data/View Binding, desugaring, Kotlin 2 Compose compiler plugin handling, Room 2.8.4, Dagger/Hilt 2.56.2 alignment, accessibility and lifecycle fixes, and newer Instabug/ML Kit native dependencies. These were not reimplemented or broadly upgraded.

The public UI settings include UI, UIData, UIDomain and nested core modules. Core includes Android, domain, data, legacy models, persistence, SQLite, booking and related modules. `JITPACK` excludes the samples. Internal TripGo consumes these source modules; no TripGo production build was used as SDK proof and no production files were changed. A TripGo-looking `toConfigDto` package resolves to source **inside this public UI repository**, not an internal module (`evidence/internal-import-check.txt`).

## Build and artifact status

Final build and test results are below; see the final runtime addendum for the installed-binary verification and limits.

| Component | Initial result | Final result | Evidence / interpretation |
|---|---|---|---|
| tripkit-android | BUILD PASS in combined build | BUILD PASS independently and combined | `initial-build.log`, `core-standalone.log`, `core-standalone-final.log`, `release-build.log` |
| tripkit-android-ui | BUILD PASS debug | BUILD PASS release | `initial-build.log`, `release-build.log` |
| tripkituisample | FAIL kapt stub generation | BUILD PASS after all source/config/inset fixes | `sample-k2-diagnostic.log`, `sample-fixed-build.log`, `sample-configured-build.log`, `sample-final-build.log`, `final-verification-retry.log` |
| Local SDK artifacts | Not independently validated | Release AAR/JAR generation passes | Main AARs in each module's `build/outputs/aar`; 11-file consumer list in `consumer/ARTIFACTS.txt` |
| Publication metadata | FAIL known-JAR packaging check | PASS generated POM check, 15 publications | `initial-*.pom`, `metadata-red.log`, `metadata-green.log` |
| Independent consumer | Offline plugin cache miss | Debug and R8 release BUILD PASS against final artifacts | `consumer-build.log`, `consumer-online.log`, `consumer-r8-hypothesis.log`, `consumer-final.log` |
| Maven repository consumption | Not established | BLOCKED by automatic approval review | Local file-only publish rejected before execution; `publication-approval.txt`. No full transitive Maven/GMM-consumer PASS is claimed. |

The core is also rechecked in its own JitPack-style settings after the final source fixes (`core-standalone-final.log`). The independent consumer is a separate Gradle root with **no project dependencies or composite build substitution**. It consumes generated release AARs/JARs, explicitly adds external dependencies extracted from generated POMs, and compiles public initialization, core Location and reusable UI references. This validates binary/resource usability but deliberately does not stand in for resolving the SDK coordinates and transitive graph from a Maven repository. Its fixture and reproduction script are under `consumer/` and `prepare_consumer.py`.

A cache miss for Gradle's transform-api artifact was resolved by one bounded online retry. Concurrent builds briefly shared compiler outputs and produced cache registration errors; the agent interrupted its two overlapping clients and switched SDK verification to sequential, in-process/non-incremental compilation. Those environmental errors are not classified as SDK defects. Logs are retained, not silently discarded.

## Runtime verification

Only existing AVD **Medium_Phone**, verified as `emulator-5554`, Android API36,
1080×2400, 4KB memory pages was used. `adb_medium.py` checks connection and AVD
identity before every operation. No physical device was targeted or AVD created.

Initial universal APK installation failed for insufficient space (255MB available,
below Android's roughly297MB reserve). One cache-trim attempt recovered no space.
An arm64-only validation APK installed after temporarily reducing only the storage
reserve to64MB, then restoring its original absent setting in a finally block.
A later update stalled PackageManager; one normal reboot preserved apps/data and
rolled back the interrupted update. One post-reboot retry succeeded. Final installed
APK SHA256 matches the final build (`patched-installed-hash.txt`), and the original
reserve setting is restored (`install-after-reboot.txt`). No app was uninstalled
or its data cleared. `emulator-after-failed-install.png` is not sample runtime proof.

| Feature | Final outcome | Evidence and limits |
|---|---|---|
| Application/SDK startup | RUNTIME PASS | `patched-launch.txt`: cold launch; new PID4637 receives HTTP200 for `/regions.json`; old PendingIntent error absent. |
| Map rendering/interaction | RUNTIME PASS | Brisbane tiles and transport markers, route framing and manual pan; `patched-03-home.*`, `patched-11-map-pan.*`. |
| Location search | RUNTIME PASS for tested query | “Central” returned “Central Coast, NSW, Australia”; `runtime-07-location-result.png`, sanitized search logs. This was before final scheduler/inset changes. |
| Region-route autocomplete | PARTIALLY VERIFIED / live FAIL | Screen/query entry works; Los Angeles “Metro” request returns HTTP409 with empty body. App remains alive; sample only logs the error and shows no useful error state. `runtime-autocomplete-query-logcat.txt`. |
| Home controller/favorites | RUNTIME PASS | Supplied favorites displayed; choosing Favorite1 and Favorite2 populates endpoints and automatically starts routing; `patched-04-favorites.*`–`patched-07-results.*`. |
| Routing/results | RUNTIME PASS for tested Brisbane pair | HTTP200, live car/motorbike/bicycle/walk/taxi results, durations/cost/emissions; `patched-routing-logcat.txt`, `patched-08-results-selected.*`. |
| Trip details/step preview | RUNTIME PASS | Walking trip:16min/1.1km, arrival/steps, route map, Go-to-preview; `patched-09-details.*`, `patched-10-preview.*`. No booking purchase or external message sent. |
| Back/background/resume | RUNTIME PASS for tested transitions | `patched-resume.txt`, `patched-final-logcat.txt`; no fatal SDK exception. Process-death restoration and rotation are not certified. |
| API36 sample insets | RUNTIME PASS | Autocomplete back control moved from y21–105 to y84–168; old/new XML and screenshots. SDK production host behavior was not changed. |
| Timetables, GPS/background location, notifications, PDF | PARTIALLY VERIFIED (source only) | A map pan was tested; no unobscured stop marker was available in the one follow-up snapshot, so no timetable selection was claimed. Permission/geofence/notification and PDF flows were not exercised. |

The menu's map button opens `HomeViewControllerActivity`; `MapActivity` also exists
but is not wired to that button. Local legitimate SDK key properties were reused
without printing or committing values. Maps/search/routing authorization is
established only for the tested flows. The new platform job had completed by the
job-state query: its absence after the successful fresh-process region response
is not a scheduling failure. Old-PID logs from the interrupted update are clearly
named `interrupted-update-*` and are not final-source runtime proof.

## Confirmed compatibility findings

| Priority | Exact issue and evidence | Correction / status |
|---|---|---|
| BLOCKER | Sample imports old `com.skedgo.tripkit.common.model.Location`; current class is in `.model.location`. Kotlin K1/kapt reports opaque module error; diagnostic K2 exposes stale imports. | Corrected three sample imports. Original normal build used as regression gate; no kapt bypass retained. |
| BLOCKER | Sample passes SharedPreferences as HttpClientModule fourth argument; current API expects activation callback. | Removed obsolete sample argument; updated README initialization. |
| BLOCKER | Sample favorite adapter assigns Long to current String `Location.id`. | Converts its existing integer IDs to String at three assignments. |
| BLOCKER | Every publication's hand-edited POM forces implementation dependencies to AAR, including Kotlin, RxJava, Gson and Dagger JARs. Several also rewrite runtime scope to compile. | Removed rewrite from all 15 publications. Gradle component metadata now owns types/scopes. RED→GREEN known-JAR metadata check. Full Maven consumer remains unverified. |
| BLOCKER | Independent R8 consumer includes optional Immutables JAX-RS provider; missing javax.ws.rs/Jackson classes cause `minifyReleaseWithR8` failure. | Corrected central dependency to same-version annotations classifier. Independent R8 release build PASS; no warning suppression or server dependencies. |
| REQUIRED | Startup region refresh fails on API36 because Firebase GooglePlayDriver creates an unflagged PendingIntent. | Internal platform JobService and JobScheduler behind unchanged scheduleAsync API. Focused RED→GREEN scheduling regression; final installed verification below. |
| REQUIRED | UI alert code requests POST_NOTIFICATIONS but SDK and original merged sample manifest omit it. | Added UI permission declaration. Existing runtime permission flow is retained; merged-manifest verification required below. |
| REQUIRED | PDF transitive arm64 libmodft2/libmodpdfium/libmodpng ELF LOAD segments align at 4096 bytes. | Outstanding 16 KB release blocker; `native-alignment.txt`. New Instabug and arm64 ML Kit binaries already show 16384 alignment. Replacing PDF implementation without validation was not attempted. |
| REQUIRED | Sample hardcodes credential placeholders despite existing local SDK property configuration. | BuildConfig + manifest placeholders now read local/Gradle/environment configuration; Places can use a separate key. Live authorization is not assumed. |
| REQUIRED | JDK 17 required by AGP/source but not pinned in JitPack config. | Added `jitpack.yml` to both repositories. Static readiness correction only; no current remote build was triggered. |
| REQUIRED | Public README claims minSdk 15/16, uses JCenter and stale initialization example. | Updated both READMEs, separated 2023 published binaries from current requirements and documented migration/configuration. |
| REQUIRED | Existing public API changes since 2023: Location package/id and HttpClientModule constructor are source/binary breaking. | Sample migrated; no further public signatures changed. Release must explicitly version/document migration or separately design compatibility shims. |
| RECOMMENDED | AGP 8.9 officially supports API 35 while checkout compiles API 36. | Build succeeds; plan a supported AGP/wrapper combination before certifying Android 16. No speculative upgrade in this task. |
| BLOCKER | Sample compile-only android-stub shadows android.R and prevents normal platform resource lookup in inset handling. | Removed stub from sample; pure-JVM core use retained. See final-verification.log failure and final-verification-retry.log. |
| REQUIRED | API36 screenshots show sample search/back/route controls under the status bar. | Added sample-only system-bar inset helper in all five hosts; final visual check below. |
| RECOMMENDED | Old CommonCore consumer keep rules name pre-migration packages; UI has no dedicated consumer rule file. | Validate serialized models and real screens in minified runtime, even if the smaller R8 smoke succeeds. No broad keep-all workaround. |
| RECOMMENDED | Sample favorites2 and3 both use ID1; getFavorite returns the first match. | Use unique fixture IDs in a follow-up (under1hour). Only favorites1→2 were runtime-tested; favorite3 resolution is not claimed. |
| RECOMMENDED | Autocomplete errors are printed, while the sample error LiveData/UI is not populated. | Add actionable sample error feedback separately; current HTTP409 handling is nonfatal but silent. |
| RECOMMENDED | Duplicate Kotlin buildscript coordinates, old support test runner dependencies, old map/Places versions, retained legacy Firebase service compatibility, stale publication credential configuration. | Age alone is not a demonstrated essential build failure. Clean up in scoped follow-ups; review/remove legacy publishing credentials and rotate if still active. |

### Android 14–16 applicability

Explicit mutable geofencing PendingIntents and immutable notification/alarm intents already exist. Foreground-service types are not a reason to add permissions indiscriminately: FetchRegionsService extends legacy JobService and does not call startForeground. Background location is declared; actual runtime flows require device verification. Region refresh now uses a permission-protected platform JobService; the legacy Firebase service remains for binary compatibility. Trip-start reminders use AlarmManager.set, not setExact, so no new exact-alarm entitlement was added. API 36 forces edge-to-edge; sample host layouts and SDK screens still need visual inset testing. The sample already opts out of predictive back. None of these source observations is a substitute for device coverage across Android versions.

Official references: [Android 14](https://developer.android.com/about/versions/14/behavior-changes-14), [Android 15](https://developer.android.com/about/versions/15/behavior-changes-15), [Android 16](https://developer.android.com/about/versions/16/behavior-changes-16), [16 KB native support](https://developer.android.com/guide/practices/page-sizes), [AGP 8.9 compatibility](https://developer.android.com/build/releases/agp-8-9-0-release-notes). Only the existing Android 16/4 KB emulator was available for this audit.

## Public API and capability inventory

| Area | Android public surface / sample coverage |
|---|---|
| Initialization | TripKitUI.buildTripKitConfig, initialize overloads, getInstance; TripKitConfigs/MainModule/HttpClientModule and core TripKit graph. Sample Application uses these APIs. |
| Reusable UI | Home controller load/newInstance, LocationSearchFragment.Builder, TripKit map, trip result/list/preview/detail fragments, timetables/service details and location pointer; not every screen has a dedicated sample menu entry. |
| Routing/services | Core routing, regions, autocomplete, realtime and persistence; UI component/service accessors on TripKitUI. UI publishes core as api dependency. |
| Customization | Resource/theme overrides, setUIFontFamily/setUIFont, config flags/date-time strings, favorite suggestion provider, custom action handler factory, default map location and bottom-sheet/back callbacks. |
| Limitations | Current source has public API migration breaks above. Runtime functionality cannot be certified from declarations. Core/UI source builds and binary consumer do not depend on TripGo's proprietary modules. |

Limited comparison with [iOS public UI documentation](https://ios.developer.tripgo.com/view-controllers/): both offer home, routing/results, departures and search. iOS documents interchangeable map layers and iPad-oriented UI; Android implementation is Google Maps based. Android has reusable fragments and callbacks, but a complete platform-neutral controller/customization contract and sample coverage are not established. This is an inventory, not an iOS repository audit or proof that every uninspected iOS feature is missing.

Separate OPTIONAL follow-up tickets: document standalone controller composition and callbacks (2–4 days); expand samples for results/details/timetables and failure handling (2–4 days); scope alternative map-provider abstraction (design spike 3–5 days); assess tablet/adaptive layouts and accessibility against explicit product requirements (3–5 days). No parity feature was implemented.

## Changed files

UI repository: `TripKitAndroidUI/build.gradle`, both `TripKitAndroidUIModules/*/build.gradle` (publication metadata); `TripKitAndroidUI/src/main/AndroidManifest.xml` (notification declaration); `tripkituisample/build.gradle`, sample manifest and `App.kt` (configuration/initialization); `AutocompleterViewModel.kt`, `HomeViewControllerActivity.kt`, `SampleFavoritesSuggestionProvider.kt` (model migration); `README.md`; new `jitpack.yml`. Added SampleWindowInsets.kt and calls from all five sample activity hosts for the confirmed API36 inset defect.

Nested core: publication rewrites removed from `CommonCoreLegacy`, `TripKitAndroid`, `TripKitData`, `TripKitDomain`, `TripKitDomainLegacy`, `ValidBookingCountData`, `ValidBookingCountDomain`, `route-persistence`, `rxlifecyclecomponents`, `sqliteutils`, `trip-kit-booking`, `trip-kit-booking-ui` build.gradle files. Updated `README.md`; new `jitpack.yml`. Added central Immutables annotations classifier, internal FetchRegionsJobService, unchanged-entry-point platform scheduling, its manifest declaration and focused scheduling test. No dependency version numbers, branches, commits or submodule pointers were changed.

Local audit files: this report, HANDOFF, PLAN, reproduction helpers, consumer fixture and evidence. They are local-only and must not be staged. Existing IDE files were untouched. No staging, commits, pushes, tags, remote publication or root application edits occurred.

## JitPack release preparation and roadmap

**P0 — before publishing (roughly 3–6 engineering days plus external QA):** resolve PDF 16 KB binaries (1–3 days); validate minified runtime serialization/screens and broaden route/permission coverage (1–2 days); complete clean Maven/GMM and POM-only consumption with transitive dependencies, publication tasks and release graph (0.5–1 day). Choose a new aligned core/UI release strategy and migration version for the existing API breaks (0.5–1 day). Existing `2.2.1.5` local Maven metadata must not be reused as a new public version.

**P1 — supported modern Android compatibility (2–4 days):** API 34/35/36 lifecycle, permissions, notification/geofence flows and system insets; supported AGP/compileSDK pairing; verify resource/serialization behavior under R8. Include minimum API 23 coverage using existing project infrastructure in a later task; this audit does not create another AVD.

**P2 — maintainability (2–5 days, separately scoped):** deduplicate buildscript/dependency declarations, remove abandoned repositories only after resolution evidence, modernize test infrastructure, review exported receiver validation and publication credentials. Avoid a wholesale architecture/DI/Compose migration.

Exact future release sequence (not executed here):

1. Review/apply the local diffs, resolve P0 findings and choose new versions. Keep both repositories' module versions aligned; preserve the intentionally selected nested core commit. Any commits/submodule-pointer updates require separate user authorization.
2. With JDK 17, regenerate release AAR/JAR, sources, POM and Gradle `.module` files using `JITPACK=true`. Run affected tests; inspect the complete publication dependency graph and sources/javadoc tasks. No publication task should depend on TripGo files or credentials.
3. After explicit local-publication approval, publish into a new disposable file repository (not an existing release directory), then assemble/debug and minify a standalone consumer using only the root UI Maven dependency. Repeat once with Gradle module metadata and once POM-only; verify all internal modules and transitive repositories.
4. On Medium Phone with sufficient storage, build/install the sample with legitimate keys, run its implemented features and lifecycle/error cases, and collect sanitized logcat/screenshots. Test serialization in the minified consumer; validate all supported native ABIs for 16 KB alignment and APK packaging.
5. Review README coordinates against the actual artifact/module names, release notes and API migration requirements. Confirm both SDK JDK configs are included. Separately authorize commits, pushes and tagging; none are authorized by this audit.
6. Only after a separately authorized future release/tag, inspect the actual remote JitPack build log and fetch its artifacts from a fresh external consumer. Historical `v2.2.1.6` success does not certify this checkout.

JitPack references: [building](https://jitpack.io/docs/BUILDING/), [Android publications](https://jitpack.io/docs/ANDROID/), [Gradle publication variants](https://developer.android.com/build/publish-library/configure-pub-variants).

## Final verification and remaining limits

| Check | Result | Evidence |
|---|---|---|
| Final standalone core build | BUILD PASS | `core-standalone-final.log`: own settings, JITPACK=true, final source. |
| Final SDK/sample build, sources/POM/GMM generation | BUILD PASS | `final-verification-retry.log`; first final attempt exposed the Android-stub conflict and is retained as `final-verification.log`. |
| Core tests | PASS111,0 failures/errors/skips | Entire core test suite, including new scheduling regression. |
| UI home-controller tests | PASS8,0 failures/errors/skips | Focused affected UI suite; not the full UI test suite. |
| Sample unit test | PASS1 | `final-tests.json`; total120 tests. |
| Final external binary consumer | Debug and R8 release BUILD PASS | `consumer-final.log`;11 generated binary artifacts,89 explicit external dependencies, no project substitution. |
| Known-JAR POM regression | PASS15 publications | `metadata-final.log`; original RED evidence retained. |
| Classifier in POM and GMM | PASS | Eight POMs use annotations classifier; `gradle-classifier-metadata.json` verifies core/UI runtime selectors. |
| Notification manifest | PASS declaration merge | `permissions-final.txt`; runtime notification permission flow not exercised. |
| Installed final APK identity | PASS | `patched-installed-hash.txt`; SHA256 `2b019e0516bcd9aacde4ceea9b3c45fc45498b6bcaf23a281c389659008f490d`. |
| Whitespace/diff review | PASS | Final `git diff --check` in both repositories; `final-git-ui.txt`, `final-git-core.txt`. |

The generated core/UI javadoc JARs contain only two metadata/directory entries;
existing build logic skips `javaDocReleaseGeneration` unless a Dokka task is
requested. Complete API documentation remains a release-preparation gap.

Most file Maven repositories still attach legacy credentials to file URLs; the
main UI publication already guards this. Normalize/verify file authentication
in the approved local-publication check. That operation was rejected before
Gradle could validate it, so no publication PASS is claimed. Existing publication
credentials were not tested against a remote registry.

Most dependency resolution reused installed Gradle caches; one bounded online consumer retry fetched an uncached plugin artifact. Empty-cache CI/repository availability was not certified. The final standalone build regenerated the domain JAR ZIP metadata; all entry bytes match the exact cached JAR used by the successful consumer (`domain-jar-content-check.txt`).

The binary consumer explicitly uses AndroidX/Jetifier, desugaring, Data Binding
and `android.nonTransitiveRClass=false`, now documented. Default non-transitive-R
consumption, minified runtime serialization and every public screen were not
separately certified. R8 compilation alone does not certify serialized model
contracts or all reflective paths.

Internal architecture documentation places product accounts/settings/DRT
composition in `:tripgosdk`, outside this SDK's settings graph. Exposing those
product screens is optional separate scope. Debug Instabug-before-init and Google
Phenotype warnings were observed; no fatal exception occurred in the tested final
flows. No telemetry/architecture rewrite was made to suppress these warnings.

Review was performed by this agent against the diff, generated metadata and runtime
evidence; no independent reviewer/subagent was used, per the user's constraint.
Builds generated `.kotlin/` diagnostics. These and this local `.ai-brain/` directory
must not be staged. Configured-credential text scanning of audit artifacts found
no matches (`secret-scan.txt`); APKs remain in normal local build directories.

### Approval-review limits

Automatic approval review rejected the local file-only publication task as an
artifact-writing publication operation (`publication-approval.txt`). It was not
executed or bypassed. Thus clean Maven-coordinate/GMM/POM-only transitive consumer
validation still needs explicit approval. All binary builds and metadata generation
were completed as a safe alternative.

An optional direct Brisbane autocomplete probe was also rejected because it would
read a local key and send an authentication header to beta. No request was sent by
that probe (`backend-probe-approval.txt`); its rejection is not a backend result.
The app's existing runtime logs establish the actual HTTP409 and successful other
flows. Ticket access reached a sign-in form; no inaccessible analysis was invented.

### Reproduction commands

Run from this UI repository, using the installed JDK17 and Android SDK. Run SDK
roots sequentially because their module output directories overlap.

```sh
# Independently tested final core
JITPACK=true ./gradlew -p tripkit-android :TripKitAndroid:assembleDebug -Pkotlin.incremental=false -Pkotlin.compiler.execution.strategy=in-process --offline --console=plain

# Final combined verification (no publication task)
./gradlew :TripKitAndroid:assembleRelease :TripKitAndroidUI:assembleRelease :tripkituisample:assembleDebug :TripKitAndroid:testDebugUnitTest :TripKitAndroidUI:testDebugUnitTest --tests '*TKUIHomeViewControllerViewModelTest' :tripkituisample:testDebugUnitTest generatePomFileForReleasePublication :TripKitAndroid:generateMetadataFileForReleasePublication :TripKitAndroidUI:generateMetadataFileForReleasePublication :TripKitDomain:generateMetadataFileForReleasePublication -I .ai-brain/23656/arm64-sample.gradle -Pkotlin.incremental=false -Pkotlin.compiler.execution.strategy=in-process --offline --console=plain

# Separate binary consumer; explicitly not a Maven-coordinate consumption test
python3 .ai-brain/23656/prepare_consumer.py
ANDROID_HOME=/Users/michaelangeloreyes/Library/Android/sdk ./gradlew -p .ai-brain/23656/consumer assembleDebug assembleRelease --offline --console=plain
python3 .ai-brain/23656/evidence/metadata-regression.py
```

`PLAN.md` retains the bounded experiments and decisions. Current release readiness
is **NO**, despite working tested core SDK/sample flows:16KB PDF support, complete
publication/consumer validation and release/API migration preparation remain.
