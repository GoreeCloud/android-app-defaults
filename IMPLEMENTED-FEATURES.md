# Android App Defaults — Implemented Features

**Record type:** Repository implemented-feature inventory  
**Repository:** `GoreeCloud/android-app-defaults`  
**Lifecycle:** Development  
**Tracking:** GitHub issue #1

## Verified Development baseline on `main`

PR #2 was squash-merged to `main` as `8afd4eecc1a374443f7a7b7da72eb19e79dfc4e1`.

The exact pre-merge candidate `a945cee18ef0924d7ef5360e5f252231c446753d` passed Android Development Foundation run #4 / `35947248482`, including the local-only manifest guard, JVM tests, Android lint, and Debug assembly.

PR #3 was squash-merged to `main` as `2de009f3c3b1288eef9374d702198438eeead731`. Its exact candidate `9a4268c08b5d442d858cebaf2d4e06a4bb552a34` passed Android Development Foundation run `35954179931`, including build/lint/unit/schema-drift checks and KVM-accelerated Android 16 execution of `SinceDatabaseRuntimeTest` with `OK (4 tests)`. Merged-main readback verified the Room source, committed schema, fail-closed manifest, and runtime workflow.

This is Development evidence only. It does not establish representative-device acceptance, GLAZE UI consumer conformance, complete Integral Platform System acceptance, Release Candidate, production, or Stable qualification.

## Repository Development foundation

- Gradle Kotlin DSL Android monorepo skeleton with `:apps:since` as the first independent application module.
- Java 17 / Kotlin / Jetpack Compose Android baseline using compile/target SDK 36 and minimum SDK 29.
- Pull-request/main Android CI covering manifest policy, JVM tests, Android lint, application/instrumentation assembly, Room-schema drift, and Android 16 runtime invariants.
- Required repository-native feature-state and changelog records.

## GoreeCloud Since — implemented Development source

### Independent Android application shell and privacy boundary

- Application identity `com.goreecloud.since`, with side-by-side Development package `com.goreecloud.since.dev`.
- User-visible installed application label **Since**.
- No manifest `INTERNET`, location, advertising, analytics, account, or background-service permission.
- Automatic Android backup is disabled pending an approved Since Everkeep/system-backup policy.
- Cleartext traffic is disabled.
- Single-activity Jetpack Compose foundation.

### Core tracker/time domain

- Permanent Event and Streak tracker kinds.
- Days, Weeks, Months, and Years display-format identities.
- Domain representations for trackers, periods, optional goals, and aggregate invariants.
- Tracker-draft validation for normalized titles/notes, IANA ZoneId validation, future-start rejection with a bounded race tolerance, streak-only goals, and bounded goal amounts.
- `@ConsistentCopyVisibility` hardening for the validated-draft constructor/copy boundary.
- Injected `Clock` and calendar-aware `TimeEngine`; elapsed values are derived from persisted instants rather than stored counters.
- JVM regression coverage for daylight-saving, month-end, leap-day, week/day/hour, validation, and reversed-clock behavior.

### Verified Room persistence foundation

- Room 3.0.3 database `since.db`, schema version 1, with compiler-generated schema committed for migration review and CI drift detection.
- `tracked_events`, `event_periods`, and `event_goals` with cascading foreign keys and unique per-event period sequence.
- App-owned SQLite setup for the one-open-period partial unique index that Room's current `Index` annotation cannot express.
- Database triggers rejecting invalid period chronology, additional periods for permanent events, and goals attached to non-streak trackers.
- Transactional aggregate creation of tracker + initial open period + optional goal with stable manual sort ordering.
- Repository mapping that keeps Room entities out of the domain/UI contract.
- Lazy application-level database/repository wiring.
- Android runtime coverage for critical invariants and persistent close/reopen behavior.

### Active persistent create/Dashboard/Details candidate

The current Development candidate additionally implements:

- Reactive active-tracker aggregate observation across tracker, period, and goal tables.
- Tracker Type Chooser for Permanent Event vs Streak.
- Create Tracker UI for normalized title/note, Start = Now/current IANA ZoneId, display format, and optional streak goal.
- Validation before mutation and transactional persistent save.
- Populated Dashboard cards with locally derived elapsed summaries and optional goal summaries.
- Tracker-card navigation into persisted Tracker Details.
- Details hero elapsed value, stored start date/time/zone, note, optional goal summary, and deliberate persisted Days/Weeks/Months/Years display-format selection.
- Minute-aligned foreground ticker because visible elapsed summaries include minutes; no persistent timer or background ticker is introduced.
- Shared application clock across repository and UI time derivation.
- Fail-closed clock inconsistency, save failure, and display-format update states.
- Android runtime coverage for validated repository creation, aggregate reactivity, and persisted display-format changes.

**Candidate boundary:** these user-facing additions are not authoritative on `main` until the exact current branch head passes the applicable build/runtime gates and the reviewed pull request is merged.

## Material limitations

Custom past-start selection, icon/accent selection, Edit Tracker, streak reset/history/statistics, full goal progress/editor behavior, archive/search/settings, export/import, recovery integration, widgets, milestone notifications, approved GLAZE UI consumer mapping, and accepted Integral Platform System integrations remain open in `PLANNED-FEATURES.md`.
