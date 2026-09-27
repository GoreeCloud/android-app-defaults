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

- Multiple alarms with create, edit, enable, disable, and delete.
- One-time and repeating schedules.
- Labels, vibration, configurable snooze.
- Exact alarm scheduling while the app is backgrounded or closed.
- Restoration after restart, package replacement, time changes, and time-zone changes.
- Alarm notification and full-screen alert integration when Android permissions allow it.
- Future work: selectable alarm sounds, gradual volume, richer dismiss/snooze interaction and acceptance evidence.

### Timers

- Hours, minutes, and seconds.
- Multiple simultaneous timers.
- Labels, start, pause, resume, reset, delete.
- Exact completion scheduling and local state preservation.
- Restart restoration for active timers.
- Future work: richer presets, widget integration, and rendered/runtime acceptance evidence.

### Stopwatch

- Start, pause, resume, reset.
- Lap tracking with individual and cumulative lap times.
- Background/process-recreation persistence using monotonic time while available, with a wall-clock fallback when the monotonic base resets.

### Personalization

- System, light, and dark themes.
- Digital/analog preference.
- 12/24-hour preference.
- Haptic preference and reduced-motion preference.
- Future work: preference application to all supported interactions and dedicated accessibility acceptance.

### Widgets and system integration

Planned after the application foundation is validated:

- Home-screen clock widgets.
- Alarm information widget.
- Timer information widget where appropriate.
- Lock-screen/system surfaces supported by Android.
- Additional notification actions and platform integration where justified.

## Privacy and permissions

The Clock manifest must not request Internet or network-state access. The initial permission surface is limited to notification delivery, vibration, device restart restoration, exact alarms, and alarm full-screen presentation. Manifest validation in CI fails if unexpected permissions are added.

All alarms, timers, stopwatch state, world-clock selections, and preferences are stored locally in app-private Android storage.

## Development status

The current foundation is Development, not Release Candidate, production, Stable, or complete GLAZE UI consumer acceptance. Source implementation and green CI do not substitute for representative physical-device alarm delivery, OEM behavior, Doze/reboot/time-zone testing, TalkBack/switch-access testing, large-text/RTL review, rendered visual acceptance, widget acceptance, signing, distribution, or release qualification.
