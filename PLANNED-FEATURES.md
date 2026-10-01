# Android App Defaults — Planned Features

## 2026-09-30 Since dashboard-search continuation

The active consolidated stacked candidate now covers the specified local Dashboard ordering set, accent-tolerant title/note search, persisted sort selection, compact populated-Dashboard spacing, and actionable-only contextual guidance. Remaining S08 work is limited to optional archived-item inclusion if intentionally enabled; representative-device accessibility/localization acceptance remains open.


## 2026-09-30 Since display-settings continuation

Default display format, seconds visibility, and the reset-confirmation preference are implemented on the active consolidated Development candidate. Reset confirmation defaults on. When disabled, the existing Reset Streak editor still requires reset time and optional reason/note review, performs the same validation, preserves History, and uses the same atomic reset mutation; only the final extra confirmation is omitted. Remaining Settings work includes reduced-motion/transparency behavior only where an approved Glaze mapping permits it and representative-device accessibility/localization acceptance.


## 2026-09-29 Since import-review continuation

A stacked Development candidate now implements the review-only half of the M3 import boundary: explicit SAF document selection, bounded strict UTF-8 input, schema-v1 structural/invariant validation, and a non-mutating summary before any future restore. Unsupported or malformed input fails closed. **No imported data can be applied yet.** Replace-style mutation, conflict handling, rollback/recovery, representative document-provider/device acceptance, and release gates remain planned.

## 2026-09-29 Since portability continuation

PR #59 now also carries the first bounded M3 portability slice: user-initiated, versioned JSON export through Android's Storage Access Framework. The export is local-only and includes current tracker state, notes, full period/reset history, goals, archive state, and timestamps. Source checkpoint `017ffc3792d31e022267a642a1b1397b29460687` passed Android Development Foundation #334 / run `36554163247`. **Backup** and applied **Restore** remain fail-closed. A stacked follow-up now adds read-only import validation/review; mutation, recovery evidence, and representative document-provider/device acceptance remain open.

## 2026-09-29 Since destructive-delete continuation

The stacked Since candidate now implements the previously open permanent-delete safeguard: deletion is available only for already archived trackers, requires an explicit second confirmation, rejects active trackers at the DAO/repository boundary, and cascades the archived tracker’s dependent period/goal rows through existing Room foreign keys. The same PR #59 line now includes the validated-source JSON export slice described above. Representative-device/accessibility acceptance, import, and validated recovery remain open, so the broader M3 ownership/recovery milestone is not complete.

## 2026-09-28 Since candidate continuation

PR #49 established the first local-management M3 slice with device-local Dashboard search and deterministic Recent/Name sorting. The stacked archive/restore candidate adds reversible local archive state using the existing Room v1 field, confirmation from Details, an archived-tracker list in Settings, and restore back to Home without deleting period history or goals. Source head `857c808ce05112f83426163cd84a96724741dfb5` passed Android Development Foundation #267 / `36507824384` with Android 16 instrumentation `OK (35 tests)`. Permanent delete safeguards, export/import, recovery, and representative-device/accessibility acceptance remain open, so M3 is not complete.

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

**State:** In progress — persistence, create/Dashboard/Details, custom-start/Edit, and automated accessibility evidence are verified and merged; remaining M1 support/acceptance work stays open.

Verified on `main`:
- Room schema v1 for `tracked_events`, `event_periods`, and `event_goals`, with committed compiler-generated schema and CI drift detection.
- Exactly one open current period per tracker enforced at SQLite boundary.
- Permanent-event single-period, period chronology, and streak-only positive-goal invariants.
- Transactional tracker + initial period + optional goal creation.
- Repository/domain mapping boundary and Android 16 runtime invariant tests.
- PR #4 persistent user flow merged as `3cc9788513c100e0ca3fedb336dd57a1b3b87e84` after exact candidate `be534fc98e1fc7020296779f0e71cd4d48ae6778` passed run `35956066300` with `SinceDatabaseRuntimeTest` `OK (8 tests)`.
- Tracker Type Chooser, validated Create Tracker and persistent save.
- Reactive populated Dashboard and tracker-card Details navigation.
- Tracker Details backed by persisted tracker/period/goal state and derived `TimeEngine`.
- Persisted display-format changes from Details.
- Shared application clock plus lifecycle-aware minute refresh for visible elapsed values.

Verified on `main` through PR #6:
- User-selected past local date/time with explicit IANA zone editing.
- Deterministic DST gap/overlap resolution.
- Edit Tracker for title, note, default display format, and open current-period start/date/time/zone.
- Closed streak history remains immutable; an edited current start cannot precede the latest closed-period end.
- Exact candidate `89775713a83726df94e9592bf81358d81772d62f` passed run `35959431091` with Android 16 `SinceDatabaseRuntimeTest` `OK (10 tests)` and was squash-merged as `48d7c8342ad17e860b521690df5c817d84d92b7b`.

Verified on `main` through PR #9:
- Explicit screen heading semantics.
- Coherent TalkBack grouping for tracker cards.
- Full-row accessible display-format radio choices and streak-goal toggle target.
- Assertive error live regions.
- One shared lifecycle-aware Dashboard minute ticker rather than one ticker coroutine per card.
- Android Compose accessibility instrumentation including the custom-past-start Create → Details → Edit persistence flow.
- Android 16 runtime instrumentation remains mandatory with KVM detection and software-acceleration fallback.
- Exact candidate `b0ccc4e1f00c089ad8257f50ffb167df4b9e7af6` passed run `35963071718` with Android 16 instrumentation `OK (13 tests)` and was squash-merged as `5b14c58af68b8748e71f6af0c462650d06e852d6`.

Verified on `main` through PR #12:
- 2× font-scale Create-editor reachability instrumentation.
- Forced RTL Create-editor reachability and heading instrumentation.
- Exact candidate `17464fd4c8fa13ed0b3123ae26b28effa01fee57` passed run `35965422146` with Android 16 instrumentation `OK (15 tests)` and was squash-merged as `634623f03954bcb1e1d53107e5187b7c724b40c4`.

Verified on `main` through PR #16 and PR #19:
- Arabic translations for the complete current 51-string UI surface, English/Arabic locale metadata, and Android RTL/resource instrumentation (`OK (16 tests)` on PR #16 exact candidate).
- Automated keyboard-focus evidence verifying Cancel → Tab → Save focus movement and Enter activation (`OK (17 tests)` on PR #19 exact candidate and exact merged-main run `35968638222`).

Still open within M1:
- Curated per-tracker icon and approved accent selection. This remains blocked on an approved Since-specific GLAZE tracker icon/accent key mapping; no product key catalog was invented. The canonical Since application launcher identity is implemented separately through GoreeCloud Branding Assets.
- Preferences DataStore is implemented on `main` only for the System/Light/Dark theme preference integrated by PR #39; additional preferences remain gated on real implemented behavior.
- Representative TalkBack and large-font visual acceptance.
- Automated keyboard-focus traversal/activation evidence is verified on `main` through PR #19; representative physical-keyboard/external-switch acceptance remains open.
- Arabic localization for the current 51-string UI surface plus packaged RTL/resource verification is verified on `main` through PR #16; native-language translation review, future-feature localization, Arabic TalkBack, and representative-device RTL acceptance remain open.
- Automated Android 16 light/dark rendered evidence and human review now exist for the principal Since flow; representative physical-device/OEM visual acceptance remains required before Stable qualification.

### M2 — Streak reset, history, and goals

**State:** In progress — goal editing/progress is integrated on `main`; reset/history/statistics remain open.

- Atomic close-current/create-next streak reset at one reset instant.
- Preserved read-only history and reset reason/note.
- Derived longest streak and reset count.
- Goal editor, current-period progress, and estimated completion: integrated by PR #32 as the bounded first M2 slice; reset/history/statistics remain open.

### M3 — Local management, portability, and recovery

**State:** Planned.

- Search and deterministic Recent/Name sorting are implemented on the active Development line. Reversible archive/restore and archived-only permanent deletion are implemented on the stacked PR #59 candidate without a Room schema migration. PR #59 also adds a versioned JSON export through Android Storage Access Framework, while PR #39 provides the bounded top-level Settings/About baseline (theme, fail-closed Backup/Restore entries, privacy/security information, app version/build status).
- Validate the PR #59 export path across representative document providers/devices and retain format compatibility as import/recovery evolves.
- Read-only fail-closed import review is implemented on the current stacked candidate. Applying data remains unavailable; prefer Replace import when mutation/recovery semantics are implemented and verified rather than inventing unsafe merge behavior.
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

The current dedicated Since Compose theme is an application-local presentation mapping informed by the Stable GLAZE UI V1.6 direction, with bounded Android-emulator rendered evidence. It is **not** by itself downstream GLAZE UI consumer conformance. The applicable approved Android mapping, human/device evidence, and repository-local conformance requirements must still be verified before any conformance claim.

### M6 — Release acceptance

**State:** Planned.

- Accessibility and rendered visual acceptance.
- Representative-device functional, performance, battery, lifecycle, and recovery evidence.
- Development distribution: source plumbing now supports a protected external persistent signer,
  monotonically increasing owner `versionCode` values, and CI package/signature provenance. Still
  required are approved protected-key provisioning, a trusted key-bearing distribution path, and
  representative-device update-in-place verification without uninstall/data loss.
- Dependency/security review, signing, artifact provenance, rollback, complete applicable platform-system assessment, Release Candidate gates, production readiness, and Stable qualification.

## Other Android App Defaults applications

The broader Android App Defaults suite remains planned. No application other than the bounded Since Development source is represented by this repository as currently implemented.
