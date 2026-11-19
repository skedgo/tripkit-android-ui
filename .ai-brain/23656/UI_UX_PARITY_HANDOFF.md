# #23656 — UI/UX parity handoff

19 November 2026, Asia/Manila. Full mapping, mechanisms and evidence: `UI_UX_PARITY_AUDIT.md`.

## Implementation scope

Implementation is confined to the UI SDK, sample and documentation. Core and the TripGo reference are unchanged. No staging, commits, branch changes, pointer updates, pushing, tagging or publication are authorized for this task.

The UI baseline is `task/23656@4a35990d`; core is `task/23656@4918f598`. Preserve the pre-existing `.gitignore`, testing-only `DeveloperPreferenceRepositoryImpl.kt` change and five IDE files. Baseline hashes/statuses are in `parity-evidence/baseline.json`.

## Implemented

- Host Material/AppCompat palette bridge with SDK resource fallbacks; fixed application-versus-host night configuration in search row colors and transport icon rendering. Provider-selected colors, branding, service circles, custom strategies and alert overlays remain supported. Existing light icon rendering is protected by the unchanged #19351 pixel regression.
- Public controller input/state persistence, restored child/callback/provider reconnection, selected route invalidation when editing labels, route subscriptions reconnected on resume, main-thread current-location updates, correct search destination semantics.
- Preview stable-trip-key/numeric identity persistence, contributor reattachment through the existing shared trip map renderer, same-trip camera preservation, ownership cleanup and unified preview back/header cleanup.
- Measured/cached Home map padding and shared obstruction API; host-themed visible map control with a 48dp target.
- Shared landscape walking/cycling directions layout uses independently scrolling header and RecyclerView columns, following the existing #19351 pattern.
- Sample System/Light/Dark controls and both-configurations purple branding, IME/system bar compatibility, unique favorite fixture IDs; README integration guidance.
- Last runtime discovery: returning from rotated/font-changed preview left Details at Loading because the shared pager's argument objects are private, non-Parcelable fields. The public wrapper now reconstructs its FromRoutes/SingleTrip/FavoriteTrip arguments and query locations through existing pager APIs. Focused tests cover all three entry paths.

## Verification results

- `parity-evidence/final-validation.log`: SDK release and sample debug/release PASS; 50 discovered tests (48 executed, 2 existing ignored; zero failures/errors) before the last Details reconnection refinement. JSON: `parity-evidence/verified/final-tests.json`.
- `parity-evidence/search-final-release-tests.log`: 19 search/theme/error tests PASS and release builds PASS. JSON: `parity-evidence/search-tests.json`.
- Earlier palette/home/route and API argument checks passed; regression RED logs and unsuccessful transition captures are retained and not counted as successful screens.
- `parity-evidence/final-restoration-validation.log`: completed successfully, 22 tests with zero failures/errors/skips and UI release/sample debug/release builds PASS; `verified/restoration-tests.json` records suite counts.
- The tested real walking flow from Favorite2 to Favorite1 (16 minutes) reached Details and street instructions in both host Light and Dark. Light while the system remained Dark verified explicit host appearance. Light normal-font screenshots precede only the final Details/header refinements; dark flow includes the corrected Details implementation.
- Preview retained the walking journey at font2 portrait/landscape; landscape instructions use the new independent scroll columns. The corrected back path loaded Details content (`verified/dark-details-font2-landscape-return.png`) instead of persistent Loading.
- Initial Preview captures preceded asynchronous geometry rendering. `verified/dark-preview-stable.png` shows actual walking geometry and the arrival marker. Complete label visibility in the small font2 landscape map viewport is still uncertified.
- Preview's small default dark header glyph was a final confirmed contrast defect. A new host-bound copied drawable correction preserves branded remote colors. RED: `preview-header-red.log`, 5 tests with 1 contrast failure. GREEN: `preview-header-final.log` and `verified/header-tests.json`, 8 tests with zero failures/errors/skips, UI release/sample debug/release successful. `verified/install-header-final.txt` records successful data-preserving install and restored storage reserve. The installed dark glyph is visibly corrected in `verified/header-dark-preview.png`. Installed runtime/identity evidence is recorded in the audit's final verification section.
- Back-stack captures show selected endpoints preserved in Route and return through Search. Sheet transitions can require additional Back/close actions; capture XML, not guessed filenames, identifies the displayed screen.

## Emulator safety and reproduction

Only use the existing Medium_Phone, serial emulator-5554, through `adb_medium.py` (identity check on every call). No physical phone, app deletion or data clearing. `parity-evidence/parity_step.py` requires a fresh hierarchy and never taps stale XML after idle failure; screenshot-based coordinates can be used only after inspecting the fresh screenshot.

Install using the existing `emulator_install.py`, recording a new evidence file and checking its actual install return code. It temporarily lowers only the low-space reserve and restores it in finally. The prior two updates succeeded and restored reserve to null. Never delete apps/data to free space.

Original device settings: font_scale=1.0, accelerometer_rotation=1, user_rotation=0, system night=yes, storage reserve=null. Final restoration evidence is `verified/device-final.json`: original system settings match and sample Follow system is checked. Test appearance choices only affect this sample.

All Gradle commands use JDK17, --offline, in-process Kotlin and the existing `arm64-sample.gradle` init script. Run SDK roots sequentially; no need to rebuild core tests or publication metadata. Runtime status/crash collection uses `parity-evidence/runtime_events.py`, which never reads credential files and records only HTTP codes/stack signatures. Do not use the old capture_log.py helper because it reads local.properties.

## Release/integration limits

No existing public signatures, dependency versions, Dagger graphs, core business logic or publication metadata were changed. Home load reconnects an existing controller in its container; host callbacks/providers remain application-owned and must be re-supplied. Legacy named-resource palettes still require appropriate day/night resource overrides. Arbitrary in-place theme mutation without Activity recreation is not certified.

Public release remains gated by the previous audit: PDF native 16KB compliance, complete Maven/GMM/POM-only transitive consumption, minified runtime/serialization QA, supported-device coverage, and release/version/API migration decisions. No publication or full compatibility audit is repeated here.

Remaining work is detailed in the audit: full short-viewport map framing, all-screen font/orientation/TalkBack/permission/error/process-death/multimodal coverage, public search timetable contract and reusable results overview map. Navigator/Compose shell migration is a separately reviewed proposal; product-specific auth/profile/booking remains excluded. Legacy named resource palettes still need day/night overrides. Public release readiness is NO; controlled integration review is appropriate with the reported limits.

## Final handoff evidence

- Exact source/resource/test/README inventory: `parity-evidence/source-inventory.txt`; hashes: `source-hashes.json`. No scoped source deletions. Two report files are this handoff and the audit. QA scripts/logs/screenshots and `.kotlin/` are disposable local evidence/build state.
- Preservation: `parity-evidence/preservation-final.json`; unchanged branches/HEADs/indexes, captured pre-existing hashes, clean core and unchanged read-only reference status. No staging/commit/push/tag/publication or pointer change.
- Final build, installed APK identity, runtime and device settings must be read from the audit's final verification section and linked evidence, which supersede earlier intermediate screenshots/logs.


Final closure: installed and local APK hashes match (`verified/installed-identity.json`); original emulator settings and Follow system are restored (`verified/device-final.json`). The final installed process logged 14 HTTP200 statuses and zero collected fatal markers (`verified/final-events.txt`). The audit is the authoritative qualified verification matrix. Implementation/report work is complete, with broader QA and release gates explicitly outstanding. No new implementation is needed merely to prepare a review; do not stage or commit under this task's authorization.
