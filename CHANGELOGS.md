# Android App Defaults — Changelogs

This repository-local record is the authoritative human-readable change history for `GoreeCloud/android-app-defaults`.

## 2026-09-23 — GoreeCloud Since Development foundation candidate

**Lifecycle:** Development candidate on `feature/since-foundation`  
**Tracking:** GitHub issue #1

### Added

- Android Gradle Kotlin DSL monorepo foundation with the first independent module at `apps/since`.
- GoreeCloud Since Android application shell using product application ID `com.goreecloud.since` and Development suffix `.dev`.
- Local-only manifest boundary with no `INTERNET` or location permission.
- Core tracker/period/goal domain representations.
- Tracker-draft validation for title/note limits, IANA time zones, future starts, and streak-only bounded goals.
- Injected-clock calendar-aware `TimeEngine` with Days, Weeks, Months, and Years decomposition and fail-closed reversed-clock behavior.
- JVM regression tests covering daylight-saving, month-end, leap-day, week/day/hour, validation, and clock-inconsistency cases.
- Initial Compose Dashboard empty state with an explicit Development boundary for not-yet-implemented persistent tracker creation.
- Baseline GitHub Actions workflow for manifest policy validation, JVM tests, Android lint, and Debug assembly.
- Mandatory `IMPLEMENTED-FEATURES.md`, `PLANNED-FEATURES.md`, and `CHANGELOGS.md` repository records.

### Boundaries

This entry records candidate source changes only. Merge, exact-head CI, representative-device behavior, persistence, GLAZE UI consumer acceptance, platform-system conformance, Release Candidate, production, and Stable status must be verified independently before being claimed.
