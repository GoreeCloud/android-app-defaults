# Android App Defaults — Implemented Features

**Record type:** Repository implemented-feature inventory  
**Repository:** `GoreeCloud/android-app-defaults`  
**Lifecycle:** Development  
**Current implementation tranche:** GoreeCloud Since foundation tracked by issue #1  
**Evidence boundary:** This record describes source implemented on the current Development candidate. It does not claim merge, release, production, representative-device, GLAZE UI consumer acceptance, complete Integral Platform System conformance, or Stable qualification until those states are separately verified.

## Repository Development foundation

- Gradle Kotlin DSL Android monorepo skeleton with `:apps:since` as the first independent application module.
- Java 17 / Kotlin / Jetpack Compose Android baseline using compile/target SDK 36 and minimum SDK 29.
- Pull-request and main-branch Android CI that statically verifies the Since permission boundary and runs Since JVM tests, Android lint, and Debug assembly.
- Required repository-native feature-state and changelog records.

## GoreeCloud Since — implemented Development source

### Independent Android application shell

- Application identity `com.goreecloud.since`, with side-by-side Development package `com.goreecloud.since.dev`.
- User-visible installed application label **Since**.
- No manifest `INTERNET`, location, advertising, analytics, account, or background-service permission.
- Single-activity Jetpack Compose shell and initial Dashboard empty state.
- Truthful Development notice when the Add action is invoked while persistent tracker creation remains unimplemented.

### Core tracker domain model

- Event and Streak tracker kinds.
- Days, Weeks, Months, and Years display-format identities.
- Domain representations for trackers, periods, and optional goals.
- Tracker-draft validation for normalized titles/notes, IANA ZoneId validation, future-start rejection with a bounded race tolerance, streak-only goals, and bounded goal amounts.

### Calendar-aware elapsed-time engine

- Injected `Clock` boundary for deterministic time behavior.
- Derived elapsed-time calculation from persisted instants and an IANA ZoneId; no running counter is required.
- Calendar-aware Years, Months, Weeks, and Days decomposition followed by time-of-day remainder.
- Fail-closed clock-inconsistency result when the observed end instant precedes the start.
- Focused JVM regression coverage for a spring daylight-saving transition, month-end arithmetic, leap-day year arithmetic, week/day/hour decomposition, and reversed-clock behavior.

## Material limitations

The current Development source does **not** yet implement Room persistence, persistent tracker creation/editing, atomic streak reset, history, goals UI, archive/search/settings, export/import, recovery integration, widgets, milestone notifications, approved GLAZE UI consumer mapping, or accepted Integral Platform System integrations. Those obligations remain authoritative in `PLANNED-FEATURES.md` and issue #1.
