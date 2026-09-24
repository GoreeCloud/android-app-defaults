# android-app-defaults

A privacy-focused monorepo of lightweight replacements for common Android default and utility apps. Each app remains independently installable and maintainable while sharing architecture, UI components, privacy controls, accessibility features, themes, utilities, and development standards.

## Current Development state

The repository is in **Development**. GoreeCloud Since is the first application module under `apps/since/`.

Authoritative `main` currently contains the bounded M0 Since foundation merged through PR #2. Persistence work remains a separate candidate on PR #3 / `feature/since-persistence`.

This branch, `feature/since-create-flow`, is a **stacked Development candidate** based on the persistence line. It adds the first persistent user flow: tracker type choice, validated tracker creation at **Now** using the current IANA time zone, optional streak goal creation, and a populated Dashboard that derives elapsed summaries from persisted timestamps through `TimeEngine`.

The current create-flow slice intentionally does **not** yet implement custom past start selection, explicit start-zone editing, icon/accent selection, Edit, Tracker Details, streak reset/history, full goal management, or broader M1–M6 acceptance.

This is not Release Candidate, production, Stable, representative-device, complete GLAZE UI consumer acceptance, or complete Integral Platform System conformance.

## Build

The current build baseline is Android Gradle Plugin 8.10.1, Kotlin 2.1.21, Java 17, compile/target SDK 36, and minimum SDK 29.

Until a verified Gradle Wrapper is added, CI bootstraps Gradle 8.11.1 explicitly.

```bash
gradle :apps:since:testDebugUnitTest :apps:since:lintDebug :apps:since:assembleDebug :apps:since:assembleDebugAndroidTest
```

## Repository records

- [Implemented features](IMPLEMENTED-FEATURES.md)
- [Planned features](PLANNED-FEATURES.md)
- [Changelogs](CHANGELOGS.md)

GitHub issue #1 tracks the active GoreeCloud Since Development implementation program.
