# android-app-defaults

A privacy-focused monorepo of lightweight replacements for common Android default and utility apps. Each app remains independently installable and maintainable while sharing architecture, UI components, privacy controls, accessibility features, themes, utilities, and development standards.

## Mandatory app repository consolidation

Camera remains consolidated under `apps/camera/`. The owner has directed Keyboard, Gallery, and Launcher to separate ownership. Keyboard now has independent source ownership in `GoreeCloud/keyboard`; Gallery will be redeveloped from new source in its own repository; Launcher source and history are transferred through the separate `GoreeCloud/launcher` migration, with final monorepo retirement contingent on that accepted destination. See [the migration record](docs/migrations/2026-09-29-mandatory-app-consolidation.md).

## Current Development state

This repository retains `apps/camera/` and `apps/clock/`. The owner has directed GoreeCloud Since to its independent `GoreeCloud/since` repository, with old `apps/since/` ownership retired in this candidate only after standalone acceptance. Historical source and development evidence remain accessible in Git history. Earlier imported Keyboard, Gallery, and Launcher were retired in the preceding stacked candidate.

**Development:** the repository split does not imply Stable or production acceptance. Since-specific source, tasks and feature tracking belong in `GoreeCloud/since` following verified migration.

## Build

The current build baseline is Android Gradle Plugin 8.10.1, Kotlin 2.1.21, Java 17, compile/target SDK 36, minimum SDK 29, Room 3.0.3, and SQLite 2.7.1.

Until a verified Gradle Wrapper is added, CI bootstraps Gradle 8.11.1 explicitly.

```bash

gradle :apps:clock:testDebugUnitTest :apps:clock:lintDebug :apps:clock:assembleDebug
```

## Repository records

- [Implemented features](IMPLEMENTED-FEATURES.md)
- [Planned features](PLANNED-FEATURES.md)
- [Changelogs](CHANGELOGS.md)

GitHub issue #1 tracks the current GoreeCloud Since Development and stabilization work.


## GoreeCloud Clock Development candidate

PR #50 adds an independently installable, offline-first Clock application without adding Internet or network-state permission. The candidate includes local digital/analog clock displays, world clocks, alarms, multiple timers, stopwatch laps, full-screen/bedside presentation, local preferences, exact Android scheduling, reboot/time/time-zone restoration, and three home-screen widgets for clock, next-alarm, and running-timer information.

Running timer elapsed-time semantics use Android's monotonic elapsed-realtime clock during a boot session, with a bounded wall-clock fallback only after reboot. This prevents manual/system wall-clock changes from incorrectly shifting an already-running timer.

The Clock candidate remains Development. Source/build validation, widgets, or emulator evidence do not establish representative-device/OEM alarm delivery, Doze/reboot acceptance, accessibility acceptance, protected Development signing/update continuity, Release Candidate, production, or Stable status.
