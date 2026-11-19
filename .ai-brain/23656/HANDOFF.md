# #23656 — SDK audit handoff

19 November 2026, Asia/Manila. **Public release readiness: NO.** See [full audit](SDK_COMPATIBILITY_AUDIT.md).

- **Current source compiles:** core standalone + combined debug/release, UI release, sample debug pass. Final standalone check: `evidence/core-standalone-final.log`. Combined build/metadata/test command: `evidence/final-verification-retry.log`.
- **Tests:** 120 pass, zero failures/errors/skips: core111, focused UI home8, sample1 (`evidence/final-tests.json`). Scheduler regression was verified RED→GREEN.
- **Sample runtime:** Android16 Medium_Phone only. Menu, map tiles/markers, location search, favorite-based Brisbane routing, real result cards, walking details and preview were exercised. HTTP200 routing evidence and screenshots are in `evidence/runtime-*.{txt,png,xml}`. The final installed APK hash matches the build; its startup/region refresh, routing/details/preview and lifecycle checks pass. Autocomplete back-button inset correction is visually verified.
- **External consumption:** independent Gradle app compiles 11 generated AAR/JAR artifacts and passes R8 with the Immutables annotations classifier. Its explicit dependency list comes from POMs. This is **partial Maven integration verification**: actual root-coordinate/transitive Maven/GMM consumption is blocked by automatic approval review of local file publication.

## Fixes

Stale sample Location imports/id types and HttpClientModule call; local key configuration; sample Android stub conflict and API36 system-bar insets; invalid dependency type/scope rewrites across15 POM publications; same-version Immutables annotations artifact for R8; missing notification declaration; platform region-refresh scheduling behind unchanged public API; JDK17 JitPack configs; both READMEs.

## Remaining gates

1. PDF native dependencies have arm64 4KB LOAD alignment; select and verify a compatible16KB implementation before release.
2. Los Angeles region-route autocomplete returns HTTP409. Other tested geocode/routing flows work; do not describe all services as unavailable or all APIs as verified.
3. Approve/complete disposable local Maven publication plus clean GMM and POM-only root-coordinate consumers. Review file-repository credential configuration. No remote publication was attempted.
4. Validate minified runtime serialization/screens and Android14/15 plus minimum-supported-device behavior; only Android16/4KB runtime was exercised. API36 uses AGP8.9, officially supported through API35.
5. Choose aligned new release versions and migration policy for existing Location/HttpClientModule API breaks. Current local Maven version2.2.1.5 is stale; latest historical UI release is v2.2.1.6 (2023). Javadoc artifacts are effectively empty.

## Preservation / reproduction

UI remains `develop@0e91ab94`; core `develop@5dbec4a7`; no pointer updates, staging, commits, pushes, tags, root app edits or subagents. Existing IDE files preserved. `.ai-brain/` and generated `.kotlin/` diagnostics are local-only.

Use `PLAN.md`, `prepare_consumer.py`, and retained logs to reproduce without repeating investigation. Run SDK Gradle builds sequentially: the two roots share nested module outputs. The emulator install helper temporarily lowers only its storage reserve and always restores it; no app/data deletion. Every ADB action must continue using `adb_medium.py` identity verification. No credentials belong in reports or commits.
