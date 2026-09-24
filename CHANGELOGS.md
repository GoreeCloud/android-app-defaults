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
- Core tracker/period/goal domain representations.
- Tracker-draft validation for title/note limits, IANA time zones, future starts, and streak-only bounded goals.
- Injected-clock calendar-aware `TimeEngine` with Days, Weeks, Months, and Years decomposition and fail-closed reversed-clock behavior.
- JVM regression tests covering daylight-saving, month-end, leap-day, week/day/hour, validation, and clock-inconsistency cases.
- Initial Compose Dashboard empty state with an explicit Development boundary for not-yet-implemented persistent tracker creation.
- Baseline GitHub Actions workflow for manifest policy validation, JVM tests, Android lint, and Debug assembly.
- Mandatory `IMPLEMENTED-FEATURES.md`, `PLANNED-FEATURES.md`, and `CHANGELOGS.md` repository records.

### Verification and integration

Exact candidate `a945cee18ef0924d7ef5360e5f252231c446753d` passed Android Development Foundation run #4 / `35947248482`. PR #2 was guarded-squash merged to `main` as `8afd4eecc1a374443f7a7b7da72eb19e79dfc4e1`, and merged source readback verified the expected files and manifest state.

No separate post-merge workflow run was visible at the integration readback checkpoint. This is Development evidence, not Release Candidate, production, representative-device, GLAZE UI consumer, complete platform-system, or Stable acceptance.

## 2026-09-23 — Since persistence foundation candidate

**Lifecycle:** Development candidate on `feature/since-persistence`  
**Tracking:** GitHub issue #1

### Added

- Room 3.0.3 / SQLite 2.7.1 persistence baseline, Room Gradle schema export, committed compiler-generated v1 schema, and CI schema-drift enforcement.
- Schema-v1 entities for `tracked_events`, `event_periods`, and `event_goals` with cascading foreign keys and unique per-event period sequence.
- App-owned SQLite setup for the one-open-period partial unique index because Room 3.0's current `Index` annotation cannot express a WHERE predicate.
- Persistence triggers for period chronology, permanent-event single-period behavior, and streak-only positive goals.
- Transactional tracker aggregate creation and repository/domain mapping boundaries.
- Lazy application-level database and repository wiring.
- API 36 instrumentation coverage for the critical database invariants, including persistent database close/reopen behavior.
- CI schema artifact upload and Android 16 runtime-invariant lane.
- PR-workflow concurrency that cancels superseded heads so stale emulator jobs do not consume validation capacity, plus build-lane Android-test APK compilation to fail instrumentation compile errors before emulator provisioning.
- Runtime CI now transfers the exact build-lane application/test APK artifacts to an API 36 default emulator image, installs them directly with ADB, and invokes AndroidJUnitRunner directly. This avoids the opaque Gradle/UTP install path that previously failed before any Since test executed while preserving the Android 16 runtime gate.

### Boundary

This entry describes candidate source only. The first persistence CI attempt (#6 / `35948301985`) failed on an overloaded Kotlin mapping reference and was corrected without rewriting history. The compiler-generated schema from the repaired build lane was retrieved and committed exactly. The persistence foundation is not authoritative on `main` until the final exact-head build/lint/unit/schema-drift/API 36 runtime validation succeeds and the reviewed pull request is merged.
