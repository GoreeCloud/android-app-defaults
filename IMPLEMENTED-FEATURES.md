# Android App Defaults — Implemented Features

**Record type:** Repository implemented-feature inventory  
**Repository:** `GoreeCloud/android-app-defaults`  
**Lifecycle:** Development  
**Tracking:** GitHub issue #1

## Verified Development baseline on `main`

PR #2 was squash-merged to `main` as `8afd4eecc1a374443f7a7b7da72eb19e79dfc4e1`.

The exact pre-merge candidate `a945cee18ef0924d7ef5360e5f252231c446753d` passed Android Development Foundation run #4 / `35947248482`, including the local-only manifest guard, JVM tests, Android lint, and Debug assembly. Merged-`main` source readback verified the expected files and fail-closed manifest state.

This is Development evidence only. It does not establish representative-device acceptance, GLAZE UI consumer conformance, complete Integral Platform System acceptance, Release Candidate, production, or Stable qualification.

## Repository Development foundation

- Gradle Kotlin DSL Android monorepo skeleton with `:apps:since` as the first independent application module.
- Java 17 / Kotlin / Jetpack Compose Android baseline using compile/target SDK 36 and minimum SDK 29.
- Pull-request and main-branch Android CI that verifies the Since permission boundary and runs Since JVM tests, Android lint, and Debug assembly.
- Required repository-native feature-state and changelog records.

## GoreeCloud Since — implemented Development source on `main`

### Independent Android application shell

- Application identity `com.goreecloud.since`, with side-by-side Development package `com.goreecloud.since.dev`.
- User-visible installed application label **Since**.
- No manifest `INTERNET`, location, advertising, analytics, account, or background-service permission.
- Automatic Android backup is disabled pending an approved Since Everkeep/system-backup policy.
- Cleartext traffic is disabled.
- Single-activity Jetpack Compose shell and initial Dashboard empty state.

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

## Active persistence candidate — PR #3 / `feature/since-persistence`

The parent persistence branch contains candidate-only Development source for Room schema v1, transactional tracker aggregate creation, database-level period/goal invariants, committed schema evidence, and Android 16 invariant tests. These items are not authoritative on `main` until PR #3 completes exact-head validation and merge/readback.

## Stacked create-flow candidate — `feature/since-create-flow`

This branch adds candidate-only Development source, dependent on the PR #3 persistence line, for:

- Persistent Dashboard observation of complete tracker aggregates behind the repository boundary.
- Dashboard populated state with stable tracker cards.
- Tracker Type Chooser for Permanent Event and Streak.
- Create Tracker flow with title, optional note, default Start = Now, current IANA ZoneId, Days/Weeks/Months/Years display format, and optional streak goal amount/unit.
- Save-time `TrackerDraftValidator` validation before database mutation.
- Atomic persistence through the parent repository transaction, followed by return to the populated Dashboard.
- Foreground-only elapsed-summary refresh derived from persisted timestamps through `TimeEngine`; no persistent timer/background service is introduced.
- Fail-closed clock-inconsistency presentation and generic save-failure presentation.
- Android runtime coverage of validated repository creation, persisted initial period/goal, aggregate observation, and exactly one open period.

**Candidate boundary:** this stacked source is not authoritative on `main` and must not be treated as merged M1 functionality until its parent persistence line is accepted, the stack is reconciled with the authoritative base, exact-head validation passes, and its own reviewed PR is merged.

## Material limitations

Custom past start date/time selection, explicit start-zone editing, icon/accent selection, Edit, Tracker Details, atomic streak reset/history, complete goal editing/progress, archive/search/settings, export/import, recovery integration, widgets, milestone notifications, approved GLAZE UI consumer mapping, and accepted Integral Platform System integrations remain open in `PLANNED-FEATURES.md`.
