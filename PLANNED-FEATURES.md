# Android App Defaults — Planned Features

**Record type:** Repository planned-feature inventory  
**Repository:** `GoreeCloud/android-app-defaults`  
**Lifecycle:** Development  
**Primary active product:** GoreeCloud Since  
**Tracking issue:** #1

## GoreeCloud Since

### M0 — Foundation stabilization

**State:** Implemented on `main` for the bounded Development foundation; broader release acceptance remains open.

- PR #2 merged as `8afd4eecc1a374443f7a7b7da72eb19e79dfc4e1`.
- Exact candidate `a945cee18ef0924d7ef5360e5f252231c446753d` passed Android Development Foundation run #4 / `35947248482`.
- Main-branch readback verified the merged source and fail-closed manifest.
- A verified Gradle Wrapper remains desirable when its binary/provenance can be introduced safely; current CI explicitly bootstraps Gradle 8.11.1.
- M0 completion is Development-only and does not imply Release Candidate, production, or Stable qualification.

### M1 — Persistent tracker fundamentals

**State:** In progress — persistence foundation verified and merged; persistent create/Dashboard/Details candidate active on `feature/since-create-dashboard`.

Verified on `main`:
- Room schema v1 for `tracked_events`, `event_periods`, and `event_goals`, with committed compiler-generated schema and CI drift detection.
- Exactly one open current period per tracker enforced at SQLite boundary.
- Permanent-event single-period, period chronology, and streak-only positive-goal invariants.
- Transactional tracker + initial period + optional goal creation.
- Repository/domain mapping boundary and Android 16 runtime invariant tests.

Active candidate:
- Tracker Type Chooser.
- Create Tracker with validation before mutation and persistent save.
- Reactive populated Dashboard.
- Tracker Details backed by persisted tracker/period/goal state and derived `TimeEngine`.
- Persisted display-format changes from Details.
- Shared application clock for repository and UI time derivation.
- Runtime tests for validated creation, aggregate reactivity, and display-format persistence.

Still open within M1:
- User-selected past start date/time and explicit zone editing.
- Curated local icon and approved accent selection.
- Edit Tracker, including current-period start editing.
- Preferences DataStore only for application preferences that are actually implemented.
- Additional UI/accessibility/runtime evidence required by those behaviors.

### M2 — Streak reset, history, and goals

**State:** Planned.

- Atomic close-current/create-next streak reset at one reset instant.
- Preserved read-only history and reset reason/note.
- Derived longest streak and reset count.
- Goal editor, current-period progress, and estimated completion.

### M3 — Local management, portability, and recovery

**State:** Planned.

- Search, sorting, archive/restore, delete safeguards, settings, and About/Privacy.
- Versioned JSON export using Android Storage Access Framework.
- Fail-closed validated import with review before mutation; prefer Replace import until safe merge semantics are fully designed.
- Room migration tests using committed historical schema files.
- Tested process-death/reboot behavior and recovery evidence sufficient for the implemented scope.

### M4 — Post-MVP Android experience

**State:** Planned / post-MVP.

- Privacy-safe home-screen widgets.
- User-selected milestone notifications with battery-conscious scheduling.
- Optional text sharing and refined adaptive layouts.

### M5 — GoreeCloud platform integration

**State:** Blocked pending implementation and accepted product-local evidence.

Evaluate and integrate all nine Integral Platform Systems as applicable: GoreeCloud Manager, Privacy Shield, Wardveil Security, Everkeep, Glaze UI, GoreeCloud Mesh, GoreeCloud Identity, GoreeCloud Policy, and GoreeCloud Observability.

The temporary Compose Material 3 substrate in the current Since Development shell is **not** GLAZE UI conformance. The current approved GLAZE UI Android mapping must be verified and adopted before any conformance claim.

### M6 — Release acceptance

**State:** Planned.

- Accessibility and rendered visual acceptance.
- Representative-device functional, performance, battery, lifecycle, and recovery evidence.
- Dependency/security review, signing, artifact provenance, rollback, complete applicable platform-system assessment, Release Candidate gates, production readiness, and Stable qualification.

## Other Android App Defaults applications

The broader Android App Defaults suite remains planned. No application other than the bounded Since Development source is represented by this repository as currently implemented.
