# Android App Defaults — Changelogs

This repository-local record is the authoritative human-readable change history for `GoreeCloud/android-app-defaults`.

## 2026-09-23 — GoreeCloud Since Development foundation

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #2

### Added

- Android Gradle Kotlin DSL monorepo foundation with the first independent module at `apps/since`.
- GoreeCloud Since Android application shell using product application ID `com.goreecloud.since` and Development suffix `.dev`.
- Local-only manifest boundary with no `INTERNET` or location permission.
- Automatic Android backup disabled pending approved Since recovery integration; cleartext traffic disabled.
- Core tracker/period/goal domain representations and calendar-aware `TimeEngine`.
- Tracker-draft validation and focused JVM regressions.
- Initial Compose Dashboard empty state.
- Baseline Android CI and repository-native feature/changelog records.

### Verification and integration

Exact candidate `a945cee18ef0924d7ef5360e5f252231c446753d` passed Android Development Foundation run #4 / `35947248482`. PR #2 was guarded-squash merged to `main` as `8afd4eecc1a374443f7a7b7da72eb19e79dfc4e1`, followed by merged-source readback.

## 2026-09-23 — Since Room persistence foundation

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #3

### Added

- Room 3.0.3 / SQLite 2.7.1 persistence baseline, schema export, committed compiler-generated v1 schema, and CI drift enforcement.
- Schema-v1 entities for `tracked_events`, `event_periods`, and `event_goals` with cascading foreign keys and unique per-event period sequence.
- App-owned SQLite one-open-period partial unique index and triggers for chronology, permanent-event single-period behavior, and streak-only positive goals.
- Transactional tracker aggregate creation and repository/domain mapping.
- Lazy application database/repository wiring.
- Android 16 runtime coverage for the critical database invariants and persistent close/reopen behavior.
- CI transport of the exact build-lane app/test APK pair into a KVM-accelerated Android 16 runtime lane.

### Verification and integration

Exact candidate `9a4268c08b5d442d858cebaf2d4e06a4bb552a34` passed Android Development Foundation run `35954179931`, including build/lint/unit/schema-drift validation and `SinceDatabaseRuntimeTest` with `OK (4 tests)`. PR #3 was squash-merged to `main` as `2de009f3c3b1288eef9374d702198438eeead731`, and merged-main readback verified the expected persistence/runtime source.

Historical failed workflow attempts remain part of the change history for the concrete defects they exposed; they are not reused as final-head evidence.

## 2026-09-23 — Since persistent creation, Dashboard, and Details

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #4

### Added

- Reactive aggregate observation across active trackers, periods, and goals.
- Tracker Type Chooser for Permanent Event and Streak.
- Create Tracker screen with validation before mutation, Start = Now/current IANA ZoneId, display format, and optional streak goal.
- Atomic repository-backed tracker/current-period/goal persistence.
- Populated Dashboard cards with lifecycle-aware minute-updated calendar-aware elapsed summaries.
- Tracker Details with selectable elapsed hero, persisted start date/time/zone, note, goal summary, and deliberate persisted display-format changes.
- Shared application clock across repository and UI calculation boundaries.
- Kotlin validated-draft copy-visibility hardening.
- Android runtime coverage for validated creation, aggregate reactivity, and display-format persistence.

### Verification and integration

Exact candidate `be534fc98e1fc7020296779f0e71cd4d48ae6778` passed Android Development Foundation run `35956066300`, including manifest guard, JVM tests, Android lint, application/instrumentation APK assembly, Room-schema drift verification, and KVM-accelerated Android 16 `SinceDatabaseRuntimeTest` with `OK (8 tests)`. PR #4 was squash-merged to `main` as `3cc9788513c100e0ca3fedb336dd57a1b3b87e84`, followed by merged-source readback.

### Boundary

Custom past-start editing, icon/accent selection, Edit, streak reset/history/statistics, full goal progress/editor, archive/search/settings, portability/recovery, GLAZE UI consumer acceptance, complete platform-system conformance, representative-device acceptance, Release Candidate, production, and Stable remain open.


## 2026-09-23 — Since custom start and Edit Tracker

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #6

### Added

- Strict local date/time plus explicit IANA ZoneId input for tracker starts.
- Explicit DST resolver: nonexistent gap times fail closed; ambiguous fall-back times use the earlier valid offset.
- Create Tracker support for user-selected past start/date/time and zone.
- Edit Tracker path from Details for title, note, current start/date/time/zone, and default display format.
- Transactional edit boundary that updates only tracker metadata and the one open current period.
- Closed-history protection preventing a current streak start from moving before the latest closed period end.
- JVM resolver regressions plus Android runtime edit/history integrity tests.

### Deferred

Icon/accent selection remains open. GLAZE UI V1.6.0 is the verified current Stable shared target, but no approved Since-specific icon/accent key catalog was verified. The candidate therefore does not invent application-specific GLAZE keys or claim downstream GLAZE consumer conformance.

### Verification and integration

Exact candidate `89775713a83726df94e9592bf81358d81772d62f` passed Android Development Foundation run `35959431091` on attempt 2, including the local-only manifest guard, JVM tests, Android lint, application/instrumentation APK assembly, Room-schema drift verification, and Android 16 `SinceDatabaseRuntimeTest` with `OK (10 tests)`. Attempt 1 failed before tests because one hosted runner did not expose readable/writable `/dev/kvm`; the exact same source was rerun without weakening validation. PR #6 was then squash-merged to `main` as `48d7c8342ad17e860b521690df5c817d84d92b7b`, followed by merged-source readback.

### Boundary

Icon/accent selection, remaining M1 accessibility/preferences work, and M2–M6 remain open. This does not establish GLAZE UI consumer acceptance, representative-device acceptance, Release Candidate, production, or Stable status.


## 2026-09-24 — Since editor accessibility and UI evidence

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #9

### Added

- Explicit accessibility heading semantics on primary Since screens.
- Merged clickable tracker-card semantics so visible title/type/elapsed/goal content is exposed as one coherent accessible unit.
- Full-row selectable display-format choices and a full-row streak-goal toggle, while suppressing redundant child radio/switch focus stops.
- Assertive live-region semantics for validation, start-input, save, and display-format update errors.
- Compose UI instrumentation coverage for headings, tracker-card click semantics, selectable display-format rows, scroll-reachable Save action, assertive validation errors, and custom-past-start Create → Details → Edit persistence.
- One shared lifecycle-aware Dashboard minute ticker replacing per-card ticker coroutines.
- Compose UI test dependencies scoped to Android instrumentation/debug builds.
- Android 16 runtime CI acceleration detection: KVM is used when available; otherwise the required instrumentation suite runs with software acceleration instead of being skipped.

### Verification and integration

Exact candidate `b0ccc4e1f00c089ad8257f50ffb167df4b9e7af6` passed Android Development Foundation run `35963071718`, including the local-only manifest guard, JVM tests, Android lint, application/instrumentation APK assembly, Room-schema drift verification, and Android 16 instrumentation `OK (13 tests)`. The runtime job now keeps instrumentation mandatory while selecting KVM when available or software acceleration otherwise. PR #9 was squash-merged to `main` as `5b14c58af68b8748e71f6af0c462650d06e852d6`; draft PR #8 was closed unmerged after its non-overlapping useful work was consolidated.

### Boundary

Representative-device TalkBack/large-font/keyboard/switch-access, localization/RTL, approved Since-specific icon/accent mapping, downstream GLAZE UI consumer acceptance, and all later M2–M6 work remain open.


## 2026-09-24 — Since large-font and RTL automated evidence

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #12

### Added

- Compose instrumentation at 2× font scale verifying primary Create-editor fields and Save remain scroll-reachable.
- Forced RTL layout-direction instrumentation verifying the Create heading, title, zone field, and Save action remain reachable.
- Coverage remains in the mandatory Android 16 instrumentation lane.

### Verification and integration

Exact candidate `17464fd4c8fa13ed0b3123ae26b28effa01fee57` passed Android Development Foundation run `35965422146`, including manifest guard, JVM tests, Android lint, application/instrumentation APK assembly, Room-schema drift verification, and Android 16 instrumentation `OK (15 tests)`. PR #12 was squash-merged to `main` as `634623f03954bcb1e1d53107e5187b7c724b40c4`.

### Boundary

This is CI-level Development evidence only. It does not establish representative-device TalkBack, visual-regression, keyboard/switch-access, translation/localization, downstream GLAZE consumer acceptance, Release Candidate, production, or Stable status.


## 2026-09-24 — Since Arabic localization

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #16

### Added

- Arabic translations for all current Since Android string resources, with the product name **Since** preserved as the product identity.
- Android locale metadata declaring English and Arabic.
- Android instrumentation that verifies an Arabic configuration resolves RTL layout direction and representative localized resources.

### Verification and integration

Exact candidate `3867afe88e6570f647b65dfa224c5b67708d998c` passed Android Development Foundation run `35967471510` with Android 16 instrumentation `OK (16 tests)`. PR #16 was squash-merged to `main` as `d204cbd4464539a15b8f1ed6e0d9e9dfef4ddbb7`.

### Boundary

Native-language translation review, representative-device RTL visual acceptance, Arabic TalkBack acceptance, physical keyboard/external-switch acceptance, downstream GLAZE UI consumer acceptance, Release Candidate, production, and Stable remain open.


## 2026-09-24 — Since keyboard-focus automated evidence

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #19

### Added

- Compose keyboard input mode instrumentation for the Create editor.
- Focus assertion on Cancel.
- Tab traversal verification from Cancel to Save.
- Enter activation verification on focused Save with the existing validation error as the observable result.

### Verification and integration

Exact candidate `bdc89ec81fae8a195e7f611efa4ca0f81c614650` passed Android Development Foundation run `35968237474` with Android 16 instrumentation `OK (17 tests)`. PR #19 was squash-merged to `main` as `a24942ce244b2182d72aadde0f57eb10bebc8f5c`. Exact merged-main run `35968638222` also passed `OK (17 tests)` and produced unexpired Development artifact `since-runtime-apks` ID `10794409287`.

### Boundary

This is automated emulator evidence only. Representative physical-keyboard/external-switch acceptance, representative-device TalkBack or large-font visual acceptance, native-language Arabic review, representative-device RTL visual acceptance, downstream GLAZE UI consumer acceptance, Release Candidate, production, and Stable remain open.
