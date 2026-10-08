# android-app-defaults

A privacy-focused monorepo of lightweight replacements for common Android default and utility apps. Each app remains independently installable and maintainable while sharing architecture, UI components, privacy controls, accessibility features, themes, utilities, and development standards.

## Current source ownership — verified October 8, 2026

The owner-directed source split was accepted through protected monorepo [PR #291](https://github.com/GoreeCloud/android-app-defaults/pull/291) and read back on `main` at `068a958074431fabecff8748c9b630ea9dfdf6b9`. **Only `apps/camera/` and `apps/clock/` remain maintained here.** Their builds and the post-merge Android 16 Clock runtime gate passed.

Keyboard belongs to the independent [`GoreeCloud/keyboard`](https://github.com/GoreeCloud/keyboard) repository. Launcher belongs to independent [`GoreeCloud/launcher`](https://github.com/GoreeCloud/launcher); the source migration merged in Launcher PR #1, and the documentation follow-up merged in PR #2. Since belongs to independent [`GoreeCloud/since`](https://github.com/GoreeCloud/since); its source and documentation migrations merged in PRs #1 and #2. The owner directed a **new-source** standalone Gallery application; the `GoreeCloud/gallery` destination has not yet been created or verified. Previous Gallery source, like the other retired module sources, remains recoverable through this repository's Git history, but is not the current development destination.

The required migrated-app CI change-detection job rejects tracked `apps/keyboard/`, `apps/gallery/`, `apps/launcher/`, and `apps/since/` reintroductions and their old root Gradle module declarations. This restriction does not prohibit preserving honest historical references within migration documents.

**Lifecycle:** all source transfers are Development-level integrations, not physical-device, persistent-signing/update-continuity, nine-platform-system, Release Candidate, Production, or Stable acceptance. Independent Launcher post-merge Android 16 test reliability remains tracked in [Launcher issue #3](https://github.com/GoreeCloud/launcher/issues/3).

See [the migration history](docs/migrations/2026-09-29-mandatory-app-consolidation.md) for the former September monorepo cutover and the October owner-directed split.

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

Former monorepo issue #1 is historical Since tracking provenance. Current Since development and ongoing product tasks are owned by `GoreeCloud/since` and GoreeCloud's authoritative Drive task records.


## GoreeCloud Clock Development candidate

PR #50 adds an independently installable, offline-first Clock application without adding Internet or network-state permission. The candidate includes local digital/analog clock displays, world clocks, alarms, multiple timers, stopwatch laps, full-screen/bedside presentation, local preferences, exact Android scheduling, reboot/time/time-zone restoration, and three home-screen widgets for clock, next-alarm, and running-timer information.

Running timer elapsed-time semantics use Android's monotonic elapsed-realtime clock during a boot session, with a bounded wall-clock fallback only after reboot. This prevents manual/system wall-clock changes from incorrectly shifting an already-running timer.

The Clock candidate remains Development. Source/build validation, widgets, or emulator evidence do not establish representative-device/OEM alarm delivery, Doze/reboot acceptance, accessibility acceptance, protected Development signing/update continuity, Release Candidate, production, or Stable status.
