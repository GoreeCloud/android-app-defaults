# Android App Defaults — Planned Features

**Record type:** Repository planned-feature inventory  
**Repository:** `GoreeCloud/android-app-defaults`  
**Lifecycle:** Development  
**Primary active product:** GoreeCloud Since  
**Tracking issue:** #1

## GoreeCloud Since

### M0 — Foundation stabilization

**State:** In progress — Development candidate implemented; independent review and integration remain pending.

- Maintain the Android monorepo skeleton and independently installable `apps/since` application module.
- Keep MVP operation local-first with no `INTERNET` permission.
- Keep dependency and build versions explicit and reproducible.
- Keep final reviewed-head CI green for unit tests, Android lint, Debug assembly, and the fail-closed manifest boundary.
- Maintain truthful Development lifecycle and repository-native feature/change records.
- Add a verified Gradle Wrapper when its binary/provenance can be introduced safely; current CI explicitly bootstraps Gradle 8.11.1.

### M1 — Persistent tracker fundamentals

**State:** Planned.

- Add Room schema v1 for trackers, periods, and goals with schema export.
- Enforce exactly one open current period per tracker and transactional aggregate creation.
- Add Preferences DataStore only for application preferences that are actually implemented.
- Implement Dashboard populated state, tracker type chooser, Create/Edit, and Tracker Details.
- Persist permanent events and streak current periods.
- Keep elapsed values derived from timestamps through the injected-clock TimeEngine.

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
- Room migration tests and tested process-death/reboot behavior.
- Recovery and portability evidence sufficient for the implemented scope.

### M4 — Post-MVP Android experience

**State:** Planned / post-MVP.

- Privacy-safe home-screen widgets.
- User-selected milestone notifications with battery-conscious scheduling.
- Optional text sharing and refined adaptive layouts.

### M5 — GoreeCloud platform integration

**State:** Blocked pending implementation and accepted product-local evidence.

Evaluate and integrate all nine Integral Platform Systems as applicable: GoreeCloud Manager, Privacy Shield, Wardveil Security, Everkeep, Glaze UI, GoreeCloud Mesh, GoreeCloud Identity, GoreeCloud Policy, and GoreeCloud Observability.

The temporary Compose Material 3 substrate in the first Since Development shell is **not** GLAZE UI conformance. The current approved GLAZE UI Android mapping must be verified and adopted before any conformance claim.

### M6 — Release acceptance

**State:** Planned.

- Accessibility and rendered visual acceptance.
- Representative-device functional, performance, battery, lifecycle, and recovery evidence.
- Dependency/security review, signing, artifact provenance, rollback, complete applicable platform-system assessment, Release Candidate gates, production readiness, and Stable qualification.

## Other Android App Defaults applications

The broader Android App Defaults suite remains planned. No application other than the bounded Since Development foundation is represented by this repository as currently implemented.
