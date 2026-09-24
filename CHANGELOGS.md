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
- Initial Compose Dashboard empty state.
- Baseline GitHub Actions workflow for manifest policy validation, JVM tests, Android lint, and Debug assembly.
- Mandatory `IMPLEMENTED-FEATURES.md`, `PLANNED-FEATURES.md`, and `CHANGELOGS.md` repository records.

### Verification and integration

Exact candidate `a945cee18ef0924d7ef5360e5f252231c446753d` passed Android Development Foundation run #4 / `35947248482`. PR #2 was guarded-squash merged to `main` as `8afd4eecc1a374443f7a7b7da72eb19e79dfc4e1`, and merged source readback verified the expected files and manifest state.

This is Development evidence, not Release Candidate, production, representative-device, GLAZE UI consumer, complete platform-system, or Stable acceptance.

## 2026-09-23 — Since persistence foundation candidate

**Lifecycle:** Development candidate on `feature/since-persistence`  
**Tracking:** GitHub issue #1, PR #3

### Added

- Room 3.0.3 / SQLite 2.7.1 persistence baseline, Room Gradle schema export, committed compiler-generated v1 schema, and CI schema-drift enforcement.
- Schema-v1 entities for `tracked_events`, `event_periods`, and `event_goals` with cascading foreign keys and unique per-event period sequence.
- App-owned SQLite setup for the one-open-period partial unique index because Room 3.0's current `Index` annotation cannot express a WHERE predicate.
- Persistence triggers for period chronology, permanent-event single-period behavior, and streak-only positive goals.
- Transactional tracker aggregate creation and repository/domain mapping boundaries.
- Lazy application-level database and repository wiring.
- API 36 instrumentation coverage for the critical database invariants, including persistent database close/reopen behavior.
- CI schema artifact upload, application/instrumentation APK compilation, and Android 16 runtime-invariant lane.
- Superseded-head workflow cancellation so stale emulator jobs do not consume final validation capacity.
- Direct runtime transport of the build-lane APK artifacts rather than treating a second Gradle/UTP build as equivalent evidence.

### Boundary

This entry describes candidate source only. Historical failed workflow attempts remain evidence for the defects they exposed and are not reused as final-head validation. PR #3 remains unmerged until its current exact-head build/runtime gates and applicable review/protection conditions pass.

## 2026-09-23 — Since persistent creation and Dashboard candidate

**Lifecycle:** Stacked Development candidate on `feature/since-create-flow`  
**Dependency:** `feature/since-persistence` / PR #3  
**Tracking:** GitHub issue #1

### Added

- Persistent Dashboard aggregate observation behind the repository/domain boundary.
- Tracker Type Chooser for Permanent Event and Streak.
- Create Tracker screen with title, optional note, Start = Now/current IANA ZoneId, Days/Weeks/Months/Years display format, and optional streak goal amount/unit.
- Validation before mutation and atomic repository-backed tracker/current-period/goal creation.
- Populated Dashboard cards with foreground-derived elapsed summaries and optional goal summaries.
- Fail-closed clock-inconsistency and save-failure presentation.
- Android runtime coverage of normalized/validated streak creation, persisted current period/goal, aggregate observation, and one-open-period behavior.
- Kotlin copy-visibility hardening for the validated draft type.

### Boundary

This is a stacked candidate, not authoritative `main` state. Custom past date/time, editable zone, icon/accent selection, Edit, Details, streak reset/history, full goal management, and remaining M1–M6 acceptance remain open. The child line must be reconciled whenever PR #3 changes and cannot be merged independently of its parent persistence foundation.
