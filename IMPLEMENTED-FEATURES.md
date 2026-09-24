# Android App Defaults — Implemented Features

**Record type:** Repository implemented-feature inventory  
**Repository:** `GoreeCloud/android-app-defaults`  
**Lifecycle:** Development  
**Tracking:** GitHub issue #1

## Verified Development baseline on `main`

PR #2 was squash-merged to `main` as `8afd4eecc1a374443f7a7b7da72eb19e79dfc4e1`.

The exact pre-merge candidate `a945cee18ef0924d7ef5360e5f252231c446753d` passed Android Development Foundation run #4 / `35947248482`, including the local-only manifest guard, JVM tests, Android lint, and Debug assembly. Merged-`main` source readback verified the expected files and fail-closed manifest state. No separate post-merge workflow run was visible at the baseline readback checkpoint.

This is Development evidence only. It does not establish representative-device acceptance, GLAZE UI consumer conformance, complete Integral Platform System acceptance, Release Candidate, production, or Stable qualification.

## Repository Development foundation

- Gradle Kotlin DSL Android monorepo skeleton with `:apps:since` as the first independent application module.
- Java 17 / Kotlin / Jetpack Compose Android baseline using compile/target SDK 36 and minimum SDK 29.
- Pull-request and main-branch Android CI that verifies the Since permission boundary and runs Since JVM tests, Android lint, and Debug assembly.
- Required repository-native feature-state and changelog records.

## GoreeCloud Since — implemented Development source

### Independent Android application shell

- Application identity `com.goreecloud.since`, with side-by-side Development package `com.goreecloud.since.dev`.
- User-visible installed application label **Since**.
- No manifest `INTERNET`, location, advertising, analytics, account, or background-service permission.
- Automatic Android backup is disabled pending an approved Since Everkeep/system-backup policy.
- Cleartext traffic is disabled.
- Single-activity Jetpack Compose shell and initial Dashboard empty state.
- Truthful Development notice when the Add action is invoked while persistent tracker creation remains incomplete.

### Core tracker domain model

- Event and Streak tracker kinds.
- Days, Weeks, Months, and Years display-format identities.
- Domain representations for trackers, periods, optional goals, and aggregate invariants.
- Tracker-draft validation for normalized titles/notes, IANA ZoneId validation, future-start rejection with a bounded race tolerance, streak-only goals, and bounded goal amounts.

### Calendar-aware elapsed-time engine

- Injected `Clock` boundary for deterministic time behavior.
- Derived elapsed-time calculation from persisted instants and an IANA ZoneId; no running counter is required.
- Calendar-aware Years, Months, Weeks, and Days decomposition followed by time-of-day remainder.
- Fail-closed clock-inconsistency result when the observed end instant precedes the start.
- Focused JVM regression coverage for a spring daylight-saving transition, month-end arithmetic, leap-day year arithmetic, week/day/hour decomposition, and reversed-clock behavior.

### Room persistence candidate on `feature/since-persistence`

The current branch adds Development-candidate source for:

- Room 3.0.3 database `since.db`, schema version 1, with the compiler-generated v1 schema committed for migration review and CI drift detection.
- `tracked_events`, `event_periods`, and `event_goals` entities using the specified column identities and cascading foreign keys.
- Unique `(event_id, sequence)` period ordering.
- App-owned SQLite setup for the one-open-period partial unique index that Room's current `Index` annotation cannot express.
- Database triggers rejecting invalid period chronology, additional periods for permanent events, and goals attached to non-streak trackers.
- Transactional aggregate creation of tracker + initial open period + optional goal with stable manual sort ordering.
- Repository mapping that keeps Room entities out of the domain/UI contract.
- Lazy application-level database/repository wiring.
- Android runtime tests intended to verify the partial-index and trigger invariants on API 36, including close/reopen behavior for the persistent database.

**Candidate boundary:** the persistence items above are not authoritative on `main` until their exact branch head passes build/lint/unit/schema-drift and API 36 runtime validation and the reviewed change is merged.

## Material limitations

Persistent tracker creation is not yet connected to the Compose UI. Edit/details flows, atomic streak reset, history, goal UI, archive/search/settings, export/import, recovery integration, widgets, milestone notifications, approved GLAZE UI consumer mapping, and accepted Integral Platform System integrations remain open in `PLANNED-FEATURES.md`.
