# SDK Compatibility Audit Execution Plan

Goal: establish independent build, artifact consumption, and Medium Phone runtime evidence for #23656; repair only reproduced compatibility blockers.
Spec: user attachment supplied in this chat, 19 November 2026 (Asia/Manila).

Constraints: preserve develop branches, commits, nested pointers and existing .idea files; no subagents, staging, commits, publication or TripGo root edits. User explicitly authorizes routine Gradle/dependency/sample fixes and unattended execution, overriding workflow approval gates. Root AI Brain checkpoint/preflight are omitted because they write outside the two authorized SDK repositories. All task records live here. No local.properties contents will be printed.

- [x] Record clean tracked baseline and existing untracked IDE files, read inherited instructions and relevant Brain summary.
- [x] Verify GitHub release and JitPack metadata separately; compare source and public artifact versions.
- [x] Run targeted core/UI/sample baseline build; isolate root causes and repair scoped configuration/API failures, using original failing command as regression check.
- [x] Build core independently, build release artifacts, inspect POM/module metadata and test an isolated external consumer without project substitutions.
- [x] Inspect SDK initialization, public UI APIs, manifests, resources, consumer rules and sample implementation.
- [x] Verify only Medium_Phone before ADB actions; build/install/run sample and exercise implemented screens, lifecycle and errors. Keep keys local; mark live functionality blocked if unavailable.
- [x] Run affected tests, final targeted builds and diff review. Document Android 14–16 applicability using official references, optional parity gaps separately, and release preparation steps.
- [x] Write SDK_COMPATIBILITY_AUDIT.md and HANDOFF.md with evidence paths and explicit limits.

Review focus: standalone settings missing parent-only configuration; POM vs module-metadata resolution; initialization without application-internal graphs; placeholder credentials vs actual crashes; API 36 insets/lifecycle and native libraries.

Confirmed findings / execution ledger:
- Debug SDK assembly passes; sample K1/kapt hides source errors behind `Could not load module <Error module>`. Disabling incremental compilation and removing android-stub independently do not resolve it; neither experiment retained.
- Diagnostic sample K2 compilation (sample kapt skipped only for diagnosis) reveals stale Location imports (class moved into `.model.location`) and a removed HttpClientModule SharedPreferences argument. Corrected the sample call sites, preserving SDK APIs. Full normal build is the regression gate.
- Generated initial POMs prove known JARs marked as AAR. Removed hand-edited types/scopes in all 15 publications, retaining `from components.*` as the authority; metadata regression was RED before edits.
- Local file Maven publication denied by automatic approval review before execution. No bypass; use AAR build and metadata validation, report full Maven publication check BLOCKED.
- Full sample compiler after the import fix reveals the same model migration changed Location.id from Long to String. Updated sample favorite adapters to stringify their existing integer IDs; no SDK public signature changed.
- Configured sample Gradle to read existing root-of-SDK local.properties or Gradle/environment key properties. Only property names were surfaced, never credential values. This follows the user's explicit instruction to inspect/reuse legitimate configuration; no local.properties files modified.
- Manifest/source check: UI alert enablement requests POST_NOTIFICATIONS, but neither public SDK manifest nor merged sample declared it. Added the missing UI manifest declaration so external hosts can grant the existing runtime request.
- Release core/UI assembly PASS; standalone core assembly PASS. Device installation failed twice due to 255 MB available, including one safe Android cache trim with no recovered space. Building an arm64-only APK as a final bounded, non-destructive attempt.
- ELF inspection finds arm64 libmodft2/libmodpdfium/libmodpng with 4096-byte LOAD alignment via the PDF dependency. Record REQUIRED/P0 before 16 KB release; no unverified replacement-library upgrade.
- Verification correction: overlapping standalone/combined builds share module outputs and triggered compiler cache registration errors. Interrupted only owned build clients 63035/63267; do not classify these errors as SDK regressions. Subsequent SDK checks run sequentially with non-incremental, in-process Kotlin compilation. Standalone binary consumer uses separate outputs.

- Runtime recovered: 35 MB arm64 sample installed after temporarily reducing only emulator low-storage reserve to64 MB, then restoring original absent setting (null). No app/data deletion. Launch and autocomplete/search screens open. Location query returns Central Coast. Region route autocomplete returns HTTP409 from beta.
- Startup proves legacy Firebase GooglePlayDriver unflagged PendingIntent failure on API36, preventing region refresh scheduling. New focused platform-scheduling test fails with expected1/actual0 before source fix. Added internal JobService and platform scheduler behind unchanged FetchRegionsService.scheduleAsync API; network/replace/retry/nonpersistent behavior retained.
- Immutables optional runtime removal caused missing annotation types; service-descriptor APK exclusions did not solve R8. Same-version annotations classifier exists publicly; testing that exact artifact instead of adding suppressions or server dependencies.
- Original core110 + focused UI8 + sample1 tests pass (119 total) before scheduler correction; evidence final-unit-tests.log and unit-test-summary.txt.
- R8 GREEN with the published `org.immutables:gson:2.8.8:annotations` classifier. Applied that classifier in the central core dependency definition, keeping the version and annotation processors. No missing-class suppression, service packaging workaround, or new server runtime dependency retained. POM consumer generator now preserves classifiers.
- Runtime screenshots establish Android16 inset defect: autocomplete and home search/back controls overlap the status bar (home Search bounds y32–103 while status bar extends roughly68px). Added a sample-only shared system-bar inset helper to all sample activity hosts. SDK/TripGo production host behavior is unchanged; validate before/after bounds and screenshots after final install.
- Final inset compilation exposed sample's compileOnly android-stub shadowing the real android.R (unresolved android.R.id). Removed that stub from the Android application; it remains where needed by pure JVM core modules. Initial final-verification.log records this failure; final-verification-retry.log is the full normal rerun gate.
- Final APK client timed out after45s; reserve restored. PackageManager dumps also timed out, screenshot returned no data, old sample PID persisted and new job was absent. Do not treat old-PID logs as final-source runtime verification. One normal, data-preserving reboot of the same verified AVD is the bounded recovery; no wipes, uninstalls or app-data clears.

Final disposition: source/build/metadata/test and final binary-consumer checks PASS; installed final APK hash matches, live primary flows verified. Maven-coordinate publication validation remains BLOCKED by automatic review; PDF16KB and release migration/coverage gates remain documented. No push/tag/release/commit/staging.

Final standalone core check PASS after all source fixes. Domain JAR ZIP metadata regenerated; every entry byte equals the cached consumer-tested JAR, so no source/binary-content regression or extra R8 rerun is implied. Release history diverges (UI2tag-only/974checkout-only; core11tag-only/524checkout-only); both UI release functional corrections are already present.
