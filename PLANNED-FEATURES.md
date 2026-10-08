# Android App Defaults — Planned Features

## September 29, 2026 — Clock widget continuation

Adaptive compact/regular sizing on Draft PR #50 passed Android Development Foundation run #281 / `36511856286` on exact source head `96fd33703c6451977b879f9ce1ad9eb852946cb8`. The stacked widget-personalization candidate adds local Settings controls for the Clock date, Next alarm secondary detail, and Timer status; all default on to preserve current behavior and compact layouts still prioritize primary content. Remaining widget work is representative-launcher resize/touch/accessibility/theme/update acceptance, supported lock-screen/system surfaces, and any future per-widget-instance configuration justified by product need.

## September 28, 2026 — relative Next alarm candidate

Draft PR #50 now enriches the already implemented exact-alarm-gated **Next alarm** surface with a tested, DST-safe relative countdown derived from the existing scheduled trigger instant. Exact candidate head `d47eb48611867b5d96b6239ea5681cbf007edbdf` passed Android Development Foundation run `36488720505`. Richer alarm presentation is therefore partially advanced, while representative-device DST/time-zone/clock-change, accessibility, delivery, sound/volume, widget, release, and Anchor acceptance remain open.


## 2026-09-28 Clock candidate continuation

Draft PR #50 now applies the existing haptic-feedback preference across previously missed presentation-mode, world-clock, permission, guidance, alarm-dialog, and timer-dialog controls. Source coverage is broader, but representative physical-device haptic feel/latency/accessibility acceptance remains open and no Stable claim is made.

**Record type:** Repository planned-feature inventory  
**Repository:** `GoreeCloud/android-app-defaults`  
**Lifecycle:** Development  
**Primary active product:** GoreeCloud Since  
**Tracking issue:** #1

## Since planning migrated

Current Since planning belongs in independent `GoreeCloud/since`. Historical obligations remain in monorepo Git history.

## Other Android App Defaults applications

The broader Android App Defaults suite remains planned. No application other than the bounded Since Development source is represented by this repository as currently implemented.


## GoreeCloud Clock — remaining work after Draft PR #50 foundation

The Clock candidate still requires the following before broader qualification:

- representative-device alarm-audio acceptance across system/default/custom alarm tones, Silent, audio focus, DND/device policy, lock-screen/full-screen presentation, snooze/dismiss, and gradual-volume behavior;
- richer alarm editing and upcoming-alarm presentation;
- representative-launcher acceptance for PR #50 adaptive sizing (exact-source CI #281 passed) and the stacked local widget-detail personalization controls; future per-widget-instance configuration remains planned only where justified;
- lock-screen/system surfaces only where supported by current Android APIs;
- full haptic-preference application across applicable controls and alerts;
- canonical GoreeCloud Clock branding from the authoritative branding repository;
- Clock-specific Android runtime/instrumentation and rendered visual evidence;
- TalkBack, Switch Access, hardware-keyboard, 200% text/reflow, RTL/localization, contrast, reduced-motion, and representative-device accessibility acceptance;
- physical-device/OEM exact-alarm, notification, full-screen-intent, reboot, Doze, wall-clock-change, time-zone-change, and DST acceptance;
- performance, memory, battery/wakeup, recovery/rollback, protected Development signing, monotonic versionCode/update-in-place, production signing/distribution, Release Candidate, production, and Stable/Anchor qualification.

No planned item in this section is represented as implemented merely because it is recorded.


### Clock onboarding acceptance still required

The Clock candidate now contains first-use onboarding and contextual-hint source, but the requirement is not fully accepted until exact-head automation and representative-device/form-factor/accessibility validation cover clean first use, interruption/resume, replay, hint dismissal/global disable/re-enable, large text, RTL/localization, keyboard/switch access, reduced motion, and upgrade persistence.
