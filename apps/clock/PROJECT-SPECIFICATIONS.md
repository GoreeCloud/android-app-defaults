# GoreeCloud Clock — Project Specifications

## Product

GoreeCloud Clock is a privacy-focused, offline-first Android clock and time-management application in the `GoreeCloud/android-app-defaults` monorepo. It is independently installable and must not require a GoreeCloud account or network service for core time-management behavior.

## Product principles

- Fast startup and responsive controls.
- Lightweight local-first implementation.
- No advertising, behavioral tracking, or unnecessary analytics.
- No Internet permission for core Clock functionality.
- Permission-minimal behavior with permissions tied to active alarm/timer capabilities.
- Accessible Android semantics, scalable text, large touch targets, logical focus order, and reduced-motion compatibility.
- Reliable alarm and timer scheduling using documented Android mechanisms instead of continuous background processing.
- Current official GLAZE UI consumer direction: 1.6.0 anchor. Consumer acceptance remains repository-local.

## Required capabilities

### Clock

- Current local time, date, and day.
- Digital and analog clock faces.
- 12-hour and 24-hour presentation.
- Full-screen and bedside modes.
- Automatic system time, time-zone, daylight-saving-time, and locale-aware behavior.

### World Clock

- Multiple saved world clocks.
- Add, remove, and reorder locations.
- Local time and UTC offset per location.
- Automatic daylight-saving-time behavior through Android/Java time-zone data.

### Alarms

- The in-app **Next alarm** summary is directly actionable in the Development candidate: selecting it opens the same existing alarm editor used by ordinary alarm cards. The interaction does not create a separate edit, persistence, or scheduling path.

- Multiple alarms with create, edit, enable, disable, and delete.
- One-time and repeating schedules.
- Labels, vibration, configurable snooze.
- Exact alarm scheduling while the app is backgrounded or closed.
- Restoration after restart, package replacement, time changes, and time-zone changes.
- Alarm notification and full-screen alert integration when Android permissions allow it.
- Selectable alarm sounds from Android alarm tones, including System default and Silent.
- Optional gradual alarm volume over 15, 30, or 60 seconds.
- The local **Next alarm** presentation includes the selected trigger time, relative time-to-trigger, label, and deterministic one-time/repeat schedule context.
- Future work: richer dismiss/snooze interaction and representative-device audio acceptance.

### Timers

- Hours, minutes, and seconds.
- Multiple simultaneous timers.
- Labels, start, pause, resume, reset, delete.
- Exact completion scheduling and local state preservation.
- Monotonic elapsed-realtime countdown behavior within the current boot so wall-clock changes do not shift active timers.
- Restart restoration for active timers using a wall-clock fallback only when the monotonic clock resets.
- Future work: richer presets and rendered/runtime acceptance evidence.

### Stopwatch

- Start, pause, resume, reset.
- Lap tracking with individual and cumulative lap times.
- Background/process-recreation persistence using monotonic time while available, with a wall-clock fallback when the monotonic base resets.
- Previous stopwatch results stored locally when a non-empty session is reset, with bounded recent-history retention and deletion/clear controls.

### Personalization

- System, light, and dark themes.
- Digital/analog preference.
- 12/24-hour preference.
- Haptic preference and reduced-motion preference.
- Haptic preference applied to primary navigation, clock-mode, alarm/timer, stopwatch, world-clock, onboarding, Settings, and alarm-alert controls using Android system haptic feedback.
- Future work: representative-device tactile/accessibility acceptance.

### First-use onboarding and contextual guidance

The Development candidate implements:

- A concise four-stage first-use flow: Welcome, Time & display, Guidance & permissions, and Ready.
- Durable local onboarding step/completion state with interruption and ordinary restart resume.
- Truthful permission/readiness explanations without requesting optional Android permissions at startup.
- Replay from Settings without deleting product data or configuration.
- Contextual hints enabled by default with a global toggle, per-hint dismissal, and reset support.
- Alarm/timer reliability hints at the relevant workflows.

Still required before acceptance:

- Representative-device first-use/recovery behavior.
- TalkBack, Switch Access, keyboard, 200% text/reflow, localization/RTL, and reduced-motion onboarding acceptance.
- Upgrade-state migration acceptance as onboarding evolves.

### Widgets and system integration

The current Development candidate implements:

- Home-screen clock widget using platform TextClock behavior.
- Home-screen next-alarm information widget.
- Home-screen running-timer information widget using platform Chronometer countdown behavior.
- Direct widget navigation into the relevant Clock, Alarms, or Timer surface.
- Local widget refresh when relevant preferences, alarms, timers, package/time/time-zone lifecycle state changes.
- Adaptive compact/regular layouts driven by launcher-provided widget size options.
- Local Settings personalization for Clock date, Next alarm secondary detail, and Timer status, with existing detail visible by default and compact layouts prioritizing primary content.

Still planned:

- Representative-launcher rendered/accessibility acceptance for resize, touch, theme, preference updates, and launcher restoration.
- Additional per-widget-instance configuration only where justified by product need.
- Lock-screen/system surfaces supported by Android.
- Additional notification actions and platform integration where justified.

## Privacy and permissions

The Clock manifest must not request Internet or network-state access. The initial permission surface is limited to notification delivery, vibration, device restart restoration, exact alarms, and alarm full-screen presentation. Manifest validation in CI fails if unexpected permissions are added.

All alarms, timers, stopwatch state, world-clock selections, and preferences are stored locally in app-private Android storage.

## Development status

The current foundation is Development, not Release Candidate, production, Stable, or complete GLAZE UI consumer acceptance. The candidate includes Android 16 instrumentation and rendered-evidence automation, but emulator evidence does not substitute for representative physical-device alarm delivery, OEM behavior, Doze/reboot/time-zone testing, TalkBack/switch-access testing, large-text/RTL review, representative launcher-widget acceptance, signing, distribution, or release qualification.


Onboarding replay and first-use completion are distinct persisted states. Starting a replay does not mark a previously oriented installation incomplete; interrupted replay may resume, and closing replay restores ordinary use without changing valid product configuration. A one-time schema migration recognizes existing Development installs with prior Clock state so introducing onboarding does not itself reset the startup experience.
