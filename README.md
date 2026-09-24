# android-app-defaults

A privacy-focused monorepo of lightweight replacements for common Android default and utility apps. Each app remains independently installable and maintainable while sharing architecture, UI components, privacy controls, accessibility features, themes, utilities, and development standards.

## Current Development state

The repository is in **Development**. GoreeCloud Since is the first application module under `apps/since/`.

Verified `main` contains the independent Android application foundation and Room persistence foundation: local-only/fail-closed manifest behavior, calendar-aware elapsed-time semantics, Room schema v1, SQLite invariants, transactional tracker aggregate creation, repository/domain mapping, committed schema evidence, and Android 16 runtime database tests.

Current `main` includes the first persistent user-facing path plus the next M1 editor slice: tracker type choice, validated persistent creation, user-selected past local date/time with explicit IANA zone input, populated Dashboard cards, persisted Tracker Details, Edit Tracker/current-period start updates, lifecycle-aware minute updates, and deliberate persisted display-format changes. Per-tracker icon/accent selection remains open because no approved Since-specific GLAZE key mapping is verified; this is separate from the canonical Since application launcher icon. Current `main` also includes the M1 accessibility/UI-evidence slice: explicit heading semantics, coherent merged tracker-card semantics, full-row radio/switch interaction targets, assertive error live regions, a single shared lifecycle-aware Dashboard minute ticker, and Android Compose tests including a custom-past-start Create → Details → Edit persistence flow. Current `main` also includes automated 2× font-scale and forced-RTL editor reachability coverage. The current Since presentation uses a dedicated light/dark GoreeCloud visual theme, layered Dashboard/tracker/detail surfaces, grouped Create/Edit sections, accessible two-column display-format choices, and explicit edge-to-edge system-bar handling; normal user-facing screens no longer expose implementation/development deferral notes. PR #24 further refined the Dashboard primary action and elapsed-time hierarchy, moved Tracker Details to a calmer neutral durable surface with focused teal emphasis, removed accent treatment that could imply a preselected tracker type, and applied consistent rounded Glaze geometry to editor fields. PR #27 extended the Android 16 rendered-evidence lane across both light and dark modes for the empty Dashboard, tracker-type chooser, Create Streak, Tracker Details, and populated Dashboard. Exact merged-main run `35986755499` / #92 passed with Android 16 instrumentation `OK (18 tests)` and published all ten exact-main full-device PNG scenes. Human review of the exact-main evidence found no clipping, text corruption, unintended pink/purple Material inheritance, or unreadable dark-mode contrast; emulator rendering remains distinct from representative physical-device/OEM and assistive-technology acceptance. PR #30 then refined the empty Dashboard from a detached floating action into a cohesive first-run card that uses the canonical Since mark, a real empty-state heading, supporting body copy, and a full-width primary `Add tracker` action while retaining the floating action for populated dashboards. Exact merged-main run `35989719467` / #98 passed Android 16 instrumentation `OK (18 tests)` and dual-theme rendered-evidence upload. Since also packages the canonical GoreeCloud Since adaptive launcher identity from `GoreeCloud/branding-assets` (`products/since/app-icon.svg`) with normal, round, and Android 13+ monochrome/themed variants; the product-local `BRANDING.md` records exact provenance. Current `main` includes Arabic translations for the complete current Since UI string surface with English/Arabic locale metadata and packaged Arabic RTL/resource verification, plus automated keyboard-focus evidence that Tab moves from Cancel to Save and Enter activates the focused Save action. PR #32 integrated the bounded M2 goal slice: calendar-aware current-period goal progress and estimated completion plus streak-only goal add/edit/remove persistence, with JVM, Android runtime, and Compose UI coverage. Native-language Arabic review, representative-device RTL/TalkBack/large-font visual acceptance, representative physical-keyboard/external-switch acceptance, per-tracker icon/accent selection, reset/history/statistics, archive/search, portability/recovery implementation, platform-system acceptance, and release gates remain open.


Current `main` also includes the PR #39 Development integration requested for everyday navigation and settings: a persistent phone bottom bar for Home, Achievements, and Settings; local-only deterministic achievements derived from existing tracker/goal data; persisted System/Light/Dark theme preference; visible Backup and Restore entries that remain intentionally fail-closed until validated recovery exists; and privacy, security, app-version, and Development-build information. PR #39 merged as `43cdfc8659de25e0d8829fed593d7f0a914e5965`; exact merged-main Android Development Foundation run `36073050400` / #153 passed with Android 16 instrumentation `OK (21 tests)` and 20-scene light/dark rendered evidence. Backup/Restore implementation, independent-review process reconciliation, representative-device acceptance, and release qualification remain separate gates.
This is not Release Candidate, production, Stable, representative-device, complete GLAZE UI consumer acceptance, or complete Integral Platform System conformance.

## Build

The current build baseline is Android Gradle Plugin 8.10.1, Kotlin 2.1.21, Java 17, compile/target SDK 36, minimum SDK 29, Room 3.0.3, and SQLite 2.7.1.

Until a verified Gradle Wrapper is added, CI bootstraps Gradle 8.11.1 explicitly.

```bash
gradle :apps:since:testDebugUnitTest :apps:since:lintDebug :apps:since:assembleDebug :apps:since:assembleDebugAndroidTest
```

## Repository records

- [Implemented features](IMPLEMENTED-FEATURES.md)
- [Planned features](PLANNED-FEATURES.md)
- [Changelogs](CHANGELOGS.md)

GitHub issue #1 tracks the current GoreeCloud Since Development and stabilization work.
