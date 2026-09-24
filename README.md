# android-app-defaults

A privacy-focused monorepo of lightweight replacements for common Android default and utility apps. Each app remains independently installable and maintainable while sharing architecture, UI components, privacy controls, accessibility features, themes, utilities, and development standards.

## Current Development state

The repository is in **Development**. GoreeCloud Since is the first application module under `apps/since/`.

Verified `main` contains the independent Android application foundation and Room persistence foundation: local-only/fail-closed manifest behavior, calendar-aware elapsed-time semantics, Room schema v1, SQLite invariants, transactional tracker aggregate creation, repository/domain mapping, committed schema evidence, and Android 16 runtime database tests.

Current `main` includes the first persistent user-facing path plus the next M1 editor slice: tracker type choice, validated persistent creation, user-selected past local date/time with explicit IANA zone input, populated Dashboard cards, persisted Tracker Details, Edit Tracker/current-period start updates, lifecycle-aware minute updates, and deliberate persisted display-format changes. Icon/accent selection remains open because no approved Since-specific GLAZE key mapping is verified. Current `main` also includes the M1 accessibility/UI-evidence slice: explicit heading semantics, coherent merged tracker-card semantics, full-row radio/switch interaction targets, assertive error live regions, a single shared lifecycle-aware Dashboard minute ticker, and Android Compose tests including a custom-past-start Create → Details → Edit persistence flow. Current `main` also includes automated 2× font-scale and forced-RTL editor reachability coverage. Current `main` also includes Arabic translations for the complete current Since UI string surface with English/Arabic locale metadata and packaged Arabic RTL/resource verification, plus automated keyboard-focus evidence that Tab moves from Cancel to Save and Enter activates the focused Save action. Native-language Arabic review, representative-device RTL/TalkBack/large-font visual acceptance, representative physical-keyboard/external-switch acceptance, icon/accent selection, reset/history, goal progress/editor, archive/search/settings, portability/recovery, platform-system acceptance, and release gates remain open.

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
