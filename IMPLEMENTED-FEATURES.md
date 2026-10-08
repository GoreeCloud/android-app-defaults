# Android App Defaults — Implemented Features


## September 29, 2026 — Clock widget detail personalization candidate

A stacked Clock Development candidate adds three app-private Settings controls for secondary home-screen widget detail: the Clock widget date, Next alarm day/readiness detail, and Timer running/readiness status. Existing users keep the current presentation by default. Preference changes reuse the existing local widget refresh path, while compact launcher sizes continue to suppress secondary detail regardless of preference so primary time/countdown content remains prioritized.

A shared pure presentation policy is unit-tested for regular/compact and enabled/disabled behavior, and Android Compose coverage verifies all three controls persist across Activity recreation. This changes presentation preferences only: no alarm/timer scheduling, persistence schema, permission, account, network, telemetry, or background authority is added. Exact-head CI and representative-launcher acceptance remain required before this tranche is accepted.


## September 29, 2026 — Clock adaptive widget sizing candidate

Draft PR #50 now makes the Clock, next-alarm, and running-timer home-screen widgets react to Android launcher resize options. A shared deterministic policy selects regular presentation at the existing default sizes and a compact presentation when the launcher grants a narrow or short surface. Compact mode hides secondary date/status detail and reduces primary type scale while preserving the existing tap destination and local-only data boundary.

The provider metadata now advertises bounded smaller resize floors so supporting launchers can actually reach the compact layout. Focused JVM coverage verifies narrow-width, short-height, default-size, and missing-option behavior. Exact source head `96fd33703c6451977b879f9ce1ad9eb852946cb8` passed Android Development Foundation run #281 / `36511856286` across Clock build/unit/lint/privacy checks, Clock Android 16 runtime instrumentation, Since build/schema regression, and Since Android 16 runtime regression. Representative-launcher resizing/accessibility acceptance remains open; no release or Stable claim is made.


## September 29, 2026 — Clock world-clock ordering persistence hardening

Draft PR #50 now routes saved World Clock add/remove/reorder mutations through one deterministic local ordering policy. Saved zone IDs are trimmed, deduplicated while preserving first occurrence, and validated with `ZoneId`; malformed local preference entries are dropped fail-closed instead of reaching the World Clock renderer. The existing saved order continues to persist through the Clock preferences store without a schema or permission change.

JVM coverage verifies normalization, add/remove/reorder behavior, and invalid-boundary moves. Android 16 instrumentation verifies a two-city reorder survives Activity recreation and that a subsequent removal is persisted. Exact source-bearing head `fe8fa6d2d293ce2da8af6ce2e11f2ddf994a1909` passed Android Development Foundation run `36510646015` / #273 across Clock build/unit/lint/manifest validation, Clock Android 16 instrumentation (**OK (6 tests)**) with rendered evidence, and both shared Since regression jobs.

This remains Development evidence. No alarm/timer scheduling, persistence schema, network/account capability, telemetry, or new permission authority changed. Representative physical-device/OEM, RTL/localization, accessibility, launcher/widget, performance/power, signing, release, production, and Stable/Anchor acceptance remain open.


## September 28, 2026 — relative Next alarm presentation

Clock's existing exact-alarm-gated **Next alarm** card now adds a compact relative-time summary such as **In 2 h 15 min**, derived from the same scheduled trigger instant rather than a second scheduling path. The presentation policy uses instant duration so DST/local-clock representation does not distort the countdown, handles sub-minute and multi-day boundaries, and fails closed to **Due now** for a non-future trigger.

Exact candidate head `d47eb48611867b5d96b6239ea5681cbf007edbdf` passed Android Development Foundation run `36488720505`, including Clock build/unit/lint, Clock Android 16 runtime, and the shared Since regression matrix. No scheduler, persistence, permission, telemetry, account, or network authority changed. Representative-device time-zone/DST/clock-change, accessibility, power, and real alarm-delivery acceptance remain open.


## September 28, 2026 — Clock next-alarm presentation candidate

Clock now derives the next scheduled enabled alarm through the same deterministic local schedule calculation used by alarm delivery and surfaces it on the Alarms screen as **Next alarm** with Today/Tomorrow/date and local-time presentation. The screen refreshes that presentation at a bounded 30-second cadence while visible; no new background service, permission, telemetry, account, or network path is introduced.

Focused unit coverage verifies earliest-occurrence selection, repeating-alarm ordering, disabled-alarm exclusion, and deterministic tie behavior through the existing schedule policy. Exact-head Android CI and representative-device time-zone/DST, clock-change, accessibility, power, and alarm-delivery acceptance remain open.


**Record type:** Repository implemented-feature inventory  
**Repository:** `GoreeCloud/android-app-defaults`  
**Lifecycle:** Development  
**Tracking:** GitHub issue #1

## Since source migrated

Since feature-state records are now maintained in `GoreeCloud/since`; historic monorepo evidence remains in earlier Git revisions. This current inventory covers the retained Clock and Camera scopes.

## GoreeCloud Clock — Draft PR #50 Development candidate

The following source is implemented on the open Clock candidate branch and is not represented as authoritative `main` until integration:

- independent Android application module `:apps:clock` with Development side-by-side package identity;
- no Internet or network-state permission, disabled automatic backup, and disabled cleartext traffic;
- digital and analog local clock displays with 12/24-hour preference, full-screen mode, and bedside mode;
- saved world clocks with add/remove/reorder, IANA time-zone handling, UTC offsets, and DST-aware Java time semantics;
- multiple local alarms with one-time/repeating schedules, labels, enable/disable/delete, vibration, configurable snooze, exact Android alarm scheduling, restart/time/time-zone restoration, notification actions, and full-screen alarm presentation;
- multiple labeled timers with start/pause/resume/reset/delete, exact completion scheduling, persisted state, and monotonic elapsed-realtime countdown semantics that are isolated from wall-clock changes during the current boot;
- stopwatch start/pause/resume/reset and persisted lap history;
- System/Light/Dark theme selection, digital/analog preference, 12/24-hour preference, haptic preference, and reduced-motion preference;
- home-screen Clock widget using platform TextClock behavior;
- home-screen next-alarm widget with local schedule/label summary and direct Alarms navigation;
- home-screen running-timer widget using platform Chronometer countdown behavior and direct Timer navigation;
- widget refresh propagation when relevant local preferences, alarms, timers, or system time/time-zone state changes;
- JVM regression coverage for alarm scheduling, timer monotonic/reboot-fallback semantics, duration formatting, stopwatch time-base behavior, and widget selection logic;
- fail-closed Clock manifest validation that prohibits network permissions and constrains exported receivers to system lifecycle and app-widget entry points.

These are Development source capabilities only. Clock-specific runtime/rendered acceptance, representative-device/OEM reliability, assistive-technology acceptance, production signing/distribution, and lifecycle promotion remain open.


### Clock Android 16 runtime and rendered-evidence candidate

Draft PR #50 now also carries a Clock-specific Android 16 instrumentation lane. The candidate instrumentation exercises top-level Clock, Alarms, Timer, Stopwatch, World, and Settings reachability plus the alarm/timer editor entry points. A dedicated rendered-evidence test captures representative light/dark Clock application scenes from Android UI automation and publishes them as a CI artifact.

This is candidate validation source until the exact head passes the new runtime workflow. Emulator evidence does not substitute for physical-device/OEM alarm delivery, launcher-widget acceptance, assistive-technology acceptance, performance/power evidence, signing/distribution, or release qualification.


### Clock first-use onboarding and contextual-guidance candidate

Draft PR #50 now includes a Clock-specific implementation of the mandatory First-Use Onboarding and Contextual Hints v1.0.0 requirement:

- four stable first-use stages: Welcome, Time & display, Guidance & permissions, and Ready;
- durable local onboarding step/completion state that survives activity/process recreation and ordinary restarts;
- no permission wall at startup: notification and exact-alarm state is explained truthfully, while native requests remain contextual to alarm/timer workflows;
- replay from Clock Settings without erasing alarms, timers, stopwatch state, world clocks, or ordinary preferences;
- contextual hints enabled by default, with a global Settings toggle, per-hint dismissal, and reset/re-enable support;
- alarm and timer reliability hints close to the relevant workflows;
- Android 16 instrumentation for first-use progression, restart/resume behavior, completion, replay, hint controls, and rendered onboarding evidence.

This remains Development source until its exact candidate head passes the complete Clock source/runtime/rendered-evidence workflow. Representative-device accessibility, localization/RTL, form-factor, recovery, and user-experience acceptance remain separate.


Replay-state hardening keeps voluntary onboarding replay separate from genuine first-use completion. Replay persists independently, resumes after interruption, can be closed without invalidating prior completion, and does not turn an experienced user's next launch into mandatory first-use. A one-time onboarding schema migration also treats pre-onboarding Development installs with existing Clock state as already oriented.


### Clock alarm audio Development candidate

Draft PR #50 now also carries per-alarm audio configuration:

- installed system alarm-sound selection with explicit System default and Silent options;
- persisted per-alarm sound selection with backward migration from the earlier alarm record format;
- configurable Off/15/30/60-second gradual volume ramp beginning at an audible floor and reaching full alarm-stream volume;
- a bounded, internal media-playback foreground service used only while an alarm is actively ringing;
- explicit alarm audio focus, USAGE_ALARM audio attributes, looping playback, per-alarm vibration lifecycle, and stop behavior shared by snooze/dismiss/full-screen alert actions;
- a dedicated silent active-alarm notification channel to prevent duplicate channel audio, plus a system-default fallback channel if active playback cannot start;
- no storage, microphone, account, Internet, or network-state permission added for alarm sound selection;
- fail-closed manifest validation for the mediaPlayback foreground-service type and internal export boundary;
- JVM volume-ramp coverage and Android 16 editor/sound-picker reachability/rendered-evidence coverage.

This remains Development source until the newer exact head passes the complete source/runtime/rendered-evidence workflow. Audible playback quality, OEM alarm-stream behavior, DND interaction, selected-tone persistence, and physical snooze/dismiss lifecycle behavior remain representative-device acceptance work.


### Clock alarm audio and haptic interaction checkpoint

Draft PR #50 now includes:

- selectable alarm sounds backed by Android alarm-tone URIs, plus System default and Silent choices;
- a looping foreground alarm playback service with alarm audio focus, fallback notification behavior, snooze/dismiss shutdown, vibration independence, and optional 15/30/60-second gradual-volume ramping;
- primary Clock interaction haptics that honor the existing Haptic feedback preference across navigation, clock-mode selection, alarm/timer controls, stopwatch actions, world-clock management, onboarding, Settings, and alarm snooze/dismiss controls;
- no new network access and no haptic-specific permission expansion; ordinary UI haptics use Android view feedback while alarm vibration remains controlled by each alarm's separate Vibrate setting.

Alarm audio/haptic implementation remains Development. Exact-head automated/runtime evidence does not replace representative-device speaker, DND/audio-focus, vibration/tactile, lock-screen, OEM, accessibility, or power acceptance.


### Clock stopwatch history candidate

Draft PR #50 now also persists previous stopwatch results locally:

- resetting any non-empty stopwatch session archives its total elapsed duration, finish timestamp, and recorded cumulative laps;
- the Stopwatch surface shows up to 20 recent results using the active 12/24-hour preference;
- individual results can be deleted and the history can be cleared without affecting the active stopwatch state;
- empty resets do not create history noise;
- history remains app-private and offline, with JVM coverage for result creation and Android 16 rendered evidence for the recent-results surface.

This history tranche requires fresh exact-head validation. Run #216 / 36419206782 remains the accepted alarm-audio/haptic checkpoint on `feae932538b23846b09e0cf3ad6591e2a2acd10b`.
