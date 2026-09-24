# android-app-defaults

A privacy-focused monorepo of lightweight replacements for common Android default and utility apps. Each app remains independently installable and maintainable while sharing architecture, UI components, privacy controls, accessibility features, themes, utilities, and development standards.

## Current Development state

The repository is in **Development**. GoreeCloud Since is the first application module being established under `apps/since/`.

Current source provides a bounded Since foundation: an independently installable Android application shell, local-only manifest boundary, initial Dashboard empty state, core tracker domain types and validation, and an injected-clock calendar-aware elapsed-time engine with focused JVM tests.

This is not Release Candidate, production, Stable, representative-device, complete GLAZE UI consumer acceptance, or complete Integral Platform System conformance.

## Build

The current build baseline is Android Gradle Plugin 8.10.1, Kotlin 2.1.21, Java 17, compile/target SDK 36, and minimum SDK 29.

Until a verified Gradle Wrapper is added, CI bootstraps Gradle 8.11.1 explicitly.

```bash
gradle :apps:since:testDebugUnitTest :apps:since:lintDebug :apps:since:assembleDebug
```

## Repository records

- [Implemented features](IMPLEMENTED-FEATURES.md)
- [Planned features](PLANNED-FEATURES.md)
- [Changelogs](CHANGELOGS.md)

GitHub issue #1 tracks the current GoreeCloud Since Development implementation tranche.
