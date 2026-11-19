# #23656 — Public UI SDK parity audit

Date: 19 November 2026 (Asia/Manila). Baseline: UI `task/23656@4a35990d`, core `task/23656@4918f598`. TripGo reference is read-only. Earlier compatibility reports describe the pre-commit checkout; Git takes precedence. Baseline status/hashes: `parity-evidence/baseline.json`. Existing `.gitignore`, developer preference testing change and IDE files are preserved.

## Component mapping and comparison (before implementation)

| Public SDK component / current implementation | Current TripGo equivalent / actual composition | Functional difference | UI/UX/theme/accessibility difference | Applicable history / priority / action |
|---|---|---|---|---|
| TKUIHomeViewControllerFragment + TKUIHomeBottomSheetFragment; map + fragment back stack | HomeFragment + BottomSheetCardsManager + navigators; same TripKitMapFragment | Public Home recreates its sheet instead of reconnecting restored children; callbacks are fields | Per-frame camera re-centering, repeated padding, no stable-state padding update; resource-backed SDK controls | #25936 viewport cancellation; #19351 shared geometry fixes already present. P1 adapt padding/stable callbacks and restoration; do not copy proprietary home/profile/accounts |
| TKUILocationSearchViewControllerFragment; XML title and LocationSearchCore child | SearchCardFragment; Compose shell and shared LocationSearchFragment | Timetable action in public wrapper is a TODO; suggestion/search engines are shared | Public close icon lacks explicit night tint; shared header already scales; shell uses legacy color aliases | #25295 Compose search shell; #25327 info action; P1 theme shell and preserve bounds/header config. P2 cosmetic shell migration |
| LocationSearchFragment/ViewModel + SearchResultRow + history/provided/fixed suggestions | Same shared classes, TripGoFavoritesSuggestionProvider and TripGoFixedSuggestionsProvider | Host supplies public favorites; no TripGo account favorites integration by design | Search rows already Compose, title/subtitle wrap at 2x; loading/empty/no-connection UI already shared | #19351 fixes already inherited. Preserve shared engines; P2 favorites management/account-specific controls |
| TKUIRouteFragment/ViewModel + TKUIRouteCardCompose → RouteCardContent | RouteCardFragment/ViewModel + RouteCardCompose → same RouteCardContent | Editing a selected nonempty label retains old coordinates; public input fields/bounds held outside arguments | Same modern Compose header already present; primary color reads SDK resource instead of host attribute; 40/24dp close/swap controls; selected home search item becomes origin | #25295 route migration already present; #24351 permission handling already present. P0 invalidate edited selection. P1 host branding, touch targets, correct destination semantics and restore inputs |
| TKUITripResultsFragment → TripResultListFragment/ViewModel | TripResultsFragment → same list/ViewModel + proprietary results map contributor | Public wrapper loses origin/destination/from-route flag on recreation; TripGo persists these | Shared cards/modes/time selection/loading/errors already current. Public results do not attach overview map contributor | #19351 shared wrapping and selected-trip restoration already present. P1 persist inputs; P2 reusable results overview contributor (avoid importing product navigator) |
| TKUITripDetailsViewControllerFragment → TripResultPagerFragment → TripSegmentListFragment/TripSegmentsViewModel | TripDetailsFragment → same pager/segments | Public wrapper loses trip identifiers; caller action factory overwritten; initial-segment coroutine outlives view | Shared walking instructions/service actions/landscape layouts already current | #19351 selection restoration exists inside shared pager but outer public wrapper discards state. P1 reconnect child/persist identifiers and scope coroutine |
| TKUITripPreviewFragment + TripPreviewPagerAdapter + TripPreviewHeaderFragment | TripPreviewPagerFragment + TripPreviewPagerAdapter/V2, same shared preview leaves and ViewModels | IDs/callbacks in fields; current page saved by shared/wrapper paths incompletely; proprietary booking streams separate | Map viewport depends on public Home; shared direction/service rows already used | #19351 shared preview geometry present. P1 retain IDs/page; restoration of all callback graphs must be verified. P2 product booking/ticket integration excluded |
| TKUITimetableControllerFragment → TimetableFragment | TripGo timetable navigator → same TimetableFragment | Different card host/callback graph | Shared scalable timetable/landscape resources already inherited | #19351 fixes already present. P2 complete public search timetable action contract |
| TKUIPoiDetailsFragment → shared POI content/map contributor; LocationPointerFragment | PoiDetailsFragment and shared LocationPointerFragment | Different public action callbacks | Existing #24773/#25692 POI map contributor changes already in SDK | Preserve; P2 broader POI/process-restoration coverage |
| TripKitUITheme + XML AppCompat/Material + public typography and RouteUiStyle | TripGoTheme + application resource overrides | Compose only resolves named SDK resources, ignores host theme colors | Sample defines `white`/`black` in values, colliding with SDK semantic aliases; Android still selects the more-specific SDK night resources (baseline screenshot confirms dark surfaces work). Using `@color/white` as an on-primary foreground therefore produces a dark foreground at night. | P1 remove sample collision; P1 bridge host theme attributes with SDK resource fallbacks; keep public signatures/style overrides |
| Sample hosts + SampleWindowInsets | TripGo app shell + WindowInsetsExtensions | Sample always reloads Home after Activity recreation | Host has no appearance controls; inset helper omits IME and bar icon appearance | P1 minimal host-controlled appearance controls and IME/system-bar handling |

### Related state and navigation classes

- `TKUIHomeViewControllerViewModel` / `TKUIHomeViewControllerUIState` correspond to the journey-planning subset of TripGo `HomeViewModel` and its navigator state, not its account/profile/DRT composition.
- `TKUILocationSearchViewControllerViewModel` only controls shell header visibility; query/suggestion/error state lives in shared `LocationSearchViewModel`. TripGo `SearchCardFragmentViewModel` additionally acquires a fresh current location and handles product configuration.
- `TKUIRouteViewModel` corresponds to `RouteCardFragmentViewModel` for endpoint/focus/swap state; the rendering leaf is already shared.
- Results, details and preview wrappers use shared `TripResultListViewModel`, `TripResultPagerViewModel`, `TripSegmentsViewModel`, `TripPreviewPagerViewModel`, `TripPreviewSharedViewModel` and `TripPreviewHeaderViewModel`. There is no need to clone those models again.
- `TKUICardBaseFragment`, `TKUICardNavigator`, `TKUICardDataManager`, `TKUICardViewControllerManager` and shared `BottomSheetCardsManager` underpin TripGo's modern `CardableFragment` host. Public Home still uses its smaller `TKUIHomeBottomSheetFragment` back stack. A wholesale navigator replacement is P2, with a migration proposal rather than silently changing host callbacks.
- `TKUICardViewController` and `TKUICardDialog` are reusable alternate public dialog hosts, not the Home screen itself. Both explicitly force `Theme.MaterialComponents.BottomSheetDialog`; P1 is to defer to the host `bottomSheetDialogTheme`, preserving an explicit `setStyle` override. The generic card XML also needs the host surface/icon attributes. Runtime coverage of this alternate host is separate from sample Home.
- Public SDK components are Fragments; sample-only Activities (`MainActivity`, `HomeViewControllerActivity`, `LocationSearchActivity`, `MapActivity`, `AutocompleterActivity`) demonstrate host composition. TripGo has its own application/navigation shell; those Activities should not be copied into the SDK.

### Historical evidence

Read current code and `git log`/`git show`, including public controller history back to #19593, route/search migrations #25295, search info #25327, permission changes #24351, POI contributor work #24773/#25692, viewport work #26159/#25936 (`c384f7ccf`, `f969387a2`), restoration #26511, and #19351 (`f22ef30d`, `a568b5af`). The November 2026 #19351 changes already cover wrapping search rows/route fields, selected-trip restoration, landscape timetable/segments, time-label sizing and measured route geometry in shared SDK classes. Public Home was not wired to all the newer host camera behavior/obstructions. Unrelated #26557 fare and #16979 meeting-point work is excluded.

## Scope/design decisions

Keep shared XML, RecyclerView, Rx2, Dagger and existing Compose leaves. Adapt public controller defects and theme bridging; do not replace screens with proprietary TripGo classes or introduce dependencies. Preserve public method signatures, all commits, branches and pointers. No root/core edits, staging, commits, publication or subagents. Root Brain commands are not run because the reference repository is explicitly read-only; task evidence remains here.

Alternative full Compose/navigation replacement would be an unnecessary public integration migration. Pure cosmetic resource copying would not repair stale selections, recreation or host branding. Use bounded compatibility fixes and document structural parity separately.

## Implementation plan

- [x] Add failing regression coverage for stale route selections, public argument persistence, host color resolution and map padding bounds.
- [x] Fix route selection invalidation and public wrapper configuration/state without public signature changes; reuse restored child fragments.
- [x] Resolve Compose palette from host theme attributes with legacy resource fallbacks, preserve RouteUiStyle/font customization, tint close/back controls and use accessible route actions.
- [x] Correct sample night resource collisions, add minimal appearance controls, respect IME/bar contrast, preserve Home on recreation.
- [x] Adapt TripGo's changed-padding/stable-sheet behavior to public Home without per-frame camera moves.
- [x] Run focused tests and sample debug/release builds as affected. Use only identity-verified existing Medium_Phone for both themes, font1/2 and orientation; bounded network retries, real responses only.
- [x] Record exact source inventory, evidence and remaining roadmap in this audit and UI_UX_PARITY_HANDOFF.md; verify original hashes/heads/indexes.

Final evidence and qualified results are recorded below.

## Confirmed implementation mechanisms

- Route text edits cleared the displayed label only when empty, retaining a previously selected nonempty location's coordinates. `setStart`/`setDestination` now invalidate a mismatching selected location. Swap exchanges selections directly; LiveData retains its existing `postValue` behavior for callers. Route subscriptions now reconnect on resume, and current-location UI updates are marshalled to the main thread.
- Public search, route, results, details and preview factories now put their serializable inputs into fragment arguments. Route editing/focus and preview page state are saved. Restored managed children are reused; listeners, suggestion providers and action factories are reattached. Callback functions remain host-owned and are not serialized. Detail initial-segment work is view-lifecycle scoped.
- Home `load` reconnects callbacks to an existing controller in the requested container. Restored sheet children are retained, sheet state is restored, and location camera defaults are not reapplied over a restored map. Search selection now fills the destination; the origin remains selectable. Headerless embedded search no longer suppresses Current Location unconditionally.
- Public Home supplies the existing shared map camera-obstruction API with its overlays. Bottom padding is measured from the sheet position, clamped to a valid viewport and applied only when changed; per-frame `moveCamera` was removed. Stable states refresh padding, and preview header height replaces the old fixed extra offset. Preview listener lookup uses the sheet's actual child manager.
- Compose resolves the host themed Context's attributes, retaining SDK named-resource fallbacks and typography. Controller XML surfaces and close icons use Material host attributes. Route actions have 48dp targets, localized descriptions and on-primary foregrounds. `RouteUiStyle` overrides remain available; custom confirm backgrounds receive contrasting text/icon colors.
- Alternate public card dialogs now defer to the host `bottomSheetDialogTheme`/explicit DialogFragment style rather than forcing a light theme. Card shell surface/tint also inherit host attributes.
- Sample appearance controls use AppCompat and persist only sample preferences. Day/night purple host resource overrides illustrate XML/Compose branding; fixed button foregrounds are separated from semantic background aliases. Sample system bar icons follow host appearance and content accounts for the IME. Favorite fixture IDs are unique.

No dependency/version, public method signature, Dagger graph, core business logic, publication metadata or submodule pointer change is required. The SDK never sets application night mode. No TripGo root file is modified.

## Regression evidence

| Check | Evidence/result |
|---|---|
| Stale coordinate selection | RED: 2 of 9 route tests failed against original code (`route-red.log`); GREEN in subsequent focused runs |
| Controller argument parcel/recreation + host surfaces | RED: 3 missing-argument cases and dark host surface failed (`controllers-theme-red.log`); GREEN subsequently |
| First implementation gate | PASS: 18 focused tests and sample debug (`focused-green-build.log`) |
| Broader implementation gate | PASS: 49 focused tests, SDK release and sample debug/release (`final-focused-build.log`); later targeted refinements require final build below |
| Public alternate dialog host | RED: forced-theme and ignored-explicit-theme tests both failed (`dialog-red-latest-debug.log`); GREEN: `final-tests.json` (51-test gate) |

All evidence paths in this section are relative to `parity-evidence/`. Builds use the existing offline caches and JDK17. Release sample is not configured as minified; no R8-runtime claim follows from assembling it. Core tests, publication and binary-consumer audit are not repeated because this change does not alter core/API signatures/dependencies/packaging.

## Runtime discoveries and bounded follow-up

The installed sample exposed two application-versus-Activity configuration bugs when the system was dark and the host explicitly chose light:

1. Search suggestion models cache ARGB values from injected application Resources. `light-search.png` shows white-on-white rows while accessibility text still exists. `final-light-search.png` after correction shows readable rows. The mapper now resolves provider-selected resource IDs using the displaying Fragment Context, retaining the old public mapper signature and provider colors without retaining an Activity in a ViewModel. Four focused host/source mismatch and custom-provider tests pass, plus 15 existing search/error tests (`search-final-release-tests.log`). The first new-test compile also exposed an incorrect nullable test fixture; that fixture was corrected. It is not reported as a behavioral RED; the before/after device screenshots establish the runtime failure and correction.
2. The injected `ThemeAwareApplyTintStrategy` similarly tinted unbranded result icons white using application night Resources. `final-light-results-loaded.png` shows blank icon areas. A View-bound adapter now resolves the host foreground on isolated Drawable copies, preserving branding, service-colored circles, configured service colors and alert overlays. The preview-header path already has a host Context and now uses it when the strategy is the SDK default. Custom strategies remain unchanged. Test compatibility had to use Robolectric's color-filter shadow because the legacy test Android stub omits the newer framework getter; no dependency/toolchain change was made.

Real text search returned Brisbane and South Brisbane station (HTTP200). City selection correctly returned to Home/map; it is not a route selection. Selecting South Brisbane station, choosing Sample Favorite2 as origin, loading real results, opening the 18-minute bicycle journey and its street-by-street preview worked in light mode. No booking/external-app action was executed.

The old UI dump helper could read a stale hierarchy after an idle failure. New local `parity-evidence/parity_step.py` uses a unique hierarchy path, rejects failed dumps and prefers text matches over map-marker descriptions. It never taps on stale XML. Transient cold-start/rotation black captures are retained as unsuccessful captures, not passed screens; a later stable landscape capture preserves the same preview/directions and shows no fatal exception in collected process logs.

An earlier landscape preview appeared to lose restored route geometry: public Home published the planned-trip event without reconnecting a shared map contributor. That wiring was repaired; the later settled capture below verifies actual geometry. Earlier broad-map captures were insufficient for a geometry verdict.


## Final implementation and compatibility

Additional runtime-driven corrections:

- Home reconnects the existing shared trip map contributor for Preview and resolves selected journeys by stable key plus numeric ID. A settled final-source dark capture (`verified/dark-preview-stable.png`) shows the real walking polyline and arrival marker. Initial asynchronous captures showed the broad Brisbane camera before the map update; they are not evidence of successful framing. Font-2 landscape has a small remaining map viewport, so complete framing of every marker/time label is not certified.
- The directions preview now has a landscape-specific layout with independently scrolling header and instructions. It retains the walking journey after font-scale and rotation recreation, rather than selecting the first mode.
- Returning to Details after that recreation initially left Loading indefinitely (`verified/light-details-font2-landscape-recheck.png`). The shared pager keeps its argument objects only in memory. The public Details wrapper reconstructs FromRoutes, SingleTrip and FavoriteTrip arguments and query locations using existing APIs. The final return capture (`verified/dark-details-font2-landscape-return.png`) contains the selected 16-minute journey and segment content. Three focused restoration tests cover those entry paths.
- The small directions header glyph used a dark default in both configurations. A host-bound preview adapter now uses a copied drawable with the host on-surface foreground for unbranded icons; branded remote icons keep their color. The initial regression failed at the missing color filter (5 tests, 1 failed, `preview-header-red.log`). Final gate and installed evidence are recorded below.

Existing public method signatures and factories remain available. The old public search row mapper signature delegates to the contextual implementation. Public theming/customization remains supported; no dependency, version, Dagger graph, core logic, packaging/publication metadata or submodule pointer was changed. Callbacks/providers are not serialized: hosts still supply their application-owned callbacks when reconnecting. This is source/build verification, not a new binary-compatibility certification.

### Exact changes

57 files: 41 modified and 16 added. `parity-evidence/source-inventory.txt` lists every added/modified source, resource, test and README file for this task. `source-hashes.json` records their final SHA-256 hashes. There are no task-scoped deletions. The two reports are `.ai-brain/23656/UI_UX_PARITY_AUDIT.md` and `UI_UX_PARITY_HANDOFF.md`. Local scripts, screenshots, test XML summaries, logs and APK identity evidence are QA artifacts under the ignored task folder, not release inputs. Existing `.gitignore`, testing-only DeveloperPreference change and IDE files are excluded. `.kotlin/` is generated build state, not a source deliverable.

### Build/test evidence

All paths below are relative to `parity-evidence/`. Counts overlap across gates; they must not be added into a unique test count.

| Gate | Actual result |
|---|---|
| Palette/dialog/Home/route/controller gate | PASS, 51 tests, zero failures/errors/skips (`final-tests.json`, `final-focused-build.log`) |
| Search context/provider/loading/error gate | PASS, 19 tests, zero failures/errors (`search-tests.json`, `search-final-release-tests.log`) |
| Preview/map/icons/layout/selection gate | PASS, 50 discovered: 48 executed, 2 existing ignored segment tests; zero failures/errors (`verified/final-tests.json`, `final-validation.log`) |
| Latest Details restoration gate | PASS, 22 tests, zero failures/errors/skips; UI SDK release and sample debug/release BUILD SUCCESSFUL (`verified/restoration-tests.json`, `final-restoration-validation.log`) |
| Preview header refinement | PASS, 8 tests, zero failures/errors/skips; SDK release and sample debug/release BUILD SUCCESSFUL (`verified/header-tests.json`, `preview-header-final.log`) |

Commands used JDK17, existing offline caches, the arm64 sample init script and in-process Kotlin. The final restoration command ran `:TripKitAndroidUI:testDebugUnitTest` with the six affected suites, `:TripKitAndroidUI:assembleRelease`, `:tripkituisample:assembleDebug` and `:tripkituisample:assembleRelease`. Core was unchanged, so core tests and the prior 120-test compatibility investigation were not repeated. Sample release is **not minified**; no R8-runtime result is claimed.

## Verification matrix

PASS is scoped to the observed sample journey and focused checks, not every public SDK entry point.

| Test | Result | Evidence / practical limit |
|---|---|---|
| SDK Home/map | PASS for launch, theme/control visibility and return; PARTIAL map framing | Both-theme Home captures, measured/clamped viewport tests; all marker extents at font2 landscape remain uncertified |
| SDK Search | PASS | Both-theme favorites/suggestions; real Brisbane text search HTTP200, South Brisbane station selection; contextual color regression tests |
| SDK Route | PASS | Correct destination semantics; Favorite2 origin/Favorite1 destination; invalidated stale coordinates; endpoint state retained on return |
| SDK Trip Results | PASS | Real motorbike/car/bicycle/walking results in both themes; readable transport icons; no fabricated backend responses |
| SDK Trip Details | PASS for tested journey | Real walking segment; font2-landscape return after correction retains the 16-minute journey |
| SDK Preview | PASS directions/selection/navigation; PARTIAL full map/accessibility certification | Real Gregory/Milton/Sylvan/Norwood instructions; stable map geometry capture; independent landscape scroll columns |
| Host light theme | PASS tested flow | Host Light while system Dark, contrasting text/icons and purple host controls; legacy semantic-resource overrides still apply |
| Host dark theme | PASS tested major screens, including corrected glyph | Host Dark, all major screen captures; corrected white header glyph verified in `verified/header-dark-preview.png`; custom theme combinations beyond sample not certified |
| Font1.0 | PASS tested major screens | Full light and dark journey paths |
| Font2.0 | PARTIAL | Preview portrait/landscape and Details return tested; exhaustive search/results/actions/long localized content not run |
| Portrait/landscape | PARTIAL | Preview and restored Details exercised; full rotation matrix for every screen not run |
| Sample build | PASS | SDK release and sample debug/release gates above; final rendering gate below |
| Sample E2E | PASS tested walking navigation; PARTIAL breadth | Search → Route → real Results → Details → Preview and back; text search also exercised. Capture filenames around back transitions can precede sheet settling; XML is authoritative |
| Independent consumer compatibility | PARTIAL / carried forward | Standalone sample has no TripGo dependency and no public API/dependency/packaging change. Fresh Maven-coordinate/GMM/POM-only consumption and minified runtime not repeated |

Only Medium_Phone (emulator-5554) was used, with identity verification on every command. No physical phone, deletion/clear-data, account changes or backend fabrication. Bounded UIAutomator idle failures and transient transition captures are retained, not reported as passed screens. Collected current-process sanitized runtime evidence reports HTTP200 responses and zero fatal exception markers; that is not exhaustive crash certification.

## Remaining roadmap

**Functional/integration gates:** no reproduced blocker remains in the tested walking screen navigation after the Details restoration correction. Map loading/framing across fresh/recreated hosts, complete label extents in the very short font2 landscape viewport, full process-death restoration, permission-denial/current-location states, offline/backend-error device flows and multimodal/service/booking entries need broader QA. Focused unit checks cover some error/state behavior but do not replace device verification.

**UI/UX parity:** public results still lack TripGo's overview contributor; public search timetable handling is a TODO. Modern TripGo uses navigator-managed cards and Compose shells around already-shared leaves. Propose a separately reviewed navigator migration preserving public host callback contracts, followed by reusable results-map and timetable contracts. Alternate public dialog hosts have theme unit coverage but were not exercised through this Home sample flow. TalkBack traversal, full contrast measurements, narrow/min-API devices, all languages and all action touch targets remain outside the tested coverage.

**Optional/product parity (P2):** shell animation/cosmetic modernization and wider shared-POI coverage. Account/profile/authentication, private favorites/business policies, ticketing/booking integrations and white-label behavior remain product-specific and are intentionally excluded. No wholesale Compose replacement or new dependency was justified.

**Carried public release blockers:** PDF native 16KB compliance, clean Maven/GMM and POM-only transitive consumption, minified runtime/serialization, supported Android/API/ABI/toolchain QA, and aligned release/version/API migration decisions. Earlier compatibility work identified existing API migration needs; this task introduces no intentional breaking signature change. Public release readiness remains **NO**. The changes are suitable for controlled integration review with these limits, not an unconditional public release approval.

## Preservation

`parity-evidence/preservation-final.json` records unchanged UI/core/reference branches, HEADs and indexes, original modified-file hashes, core cleanliness and unchanged reference status. Source changes remain unstaged. No commits, pushes, tags, publication, branch switching or pointer changes occurred. The testing-only DeveloperPreference modification and all pre-existing unrelated changes remain intact. Emulator restoration and final APK identity are recorded with the final verification below.


## Final verification closure

- `preview-header-final.log`: BUILD SUCCESSFUL, SDK UI release and sample debug/release; 8 tests, zero failures/errors/skips (`verified/header-tests.json`). The new contrast test first failed against the original header. The rebuilt APK completed a new real 16-minute walking journey in Dark; `verified/header-dark-preview.png` visibly shows the corrected white glyph and readable directions. Its initial map capture precedes geometry rendering; use the settled geometry evidence above for that separate check. The last refinement changes only the preview icon binding, not the verified Details or map logic.
- `verified/install-header-final.txt`: install return code0, Success, original storage reserve restored to null. No app/data deletion. Final installed base.apk matches the local debug APK SHA-256 `1aa89efa1e390129c917e9df96cb5ee6ca4a060b57d1eb9d384e967d41f094d9` (`verified/installed-identity.json`).
- `verified/device-final.json` and `restored-sample-menu.xml`: font1.0, user rotation0, accelerometer rotation1, night=yes, reserve=null; sample Follow system checked. Standard activity navigation returned to the sample menu; app accounts/data were preserved.
- `verified/final-events.txt`: current final-build process, 14 HTTP200 status entries and zero fatal markers in collected logs. No raw request/credential logs were copied by this collector.
- Final `git diff --check` and source hash/preservation checks passed. Both SDK indexes remain empty, branches/HEADs/pointers unchanged; core clean and TripGo reference status/captured original modified-file hashes unchanged. Existing developer testing/IDE/.gitignore work remains untouched.

The bounded implementation and requested reports are complete. Verification remains qualified as shown in the matrix; this does not certify every SDK screen/theme/device or authorize a release.
