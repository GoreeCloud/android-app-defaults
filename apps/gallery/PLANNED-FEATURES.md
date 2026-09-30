# GoreeCloud Gallery — Planned Features and Open Obligations

## September 29, 2026 — timeline grouping continuation

The current candidate advances the richer-browsing backlog with local **Day / Month / Year / None** grouping plus a persisted **Newest first / Oldest first** presentation preference over the same authorized media snapshot. Day and Newest first remain migration-safe defaults. Settings portability includes grouping and sort order, and the same continuation restores the previously exported-but-not-imported `viewDensity` value. Broader timeline navigation, additional layout controls, and representative-device/adaptive-layout acceptance remain open. Fresh exact-head validation is required.

## September 28, 2026 — bounded slideshow candidate

PR #100 now implements a bounded local photo-only slideshow in the authorized viewer. It advances forward through later images, skips non-photo media, stops at the end, and cancels on lifecycle exit or manual navigation without expanding media authority. Exact candidate head `c30ba640e86f9e4b28f78f9ad7e8b80644eec00f` passed all configured Gallery workflow families. Representative-device/accessibility/form-factor acceptance and broader presentation actions remain open.


## 2026-09-28 candidate continuation

PR #100 now includes a persisted Dense/Comfortable/Spacious media-grid presentation control. The setting is included in Gallery settings import/export and has pure policy tests. It advances the view-density backlog without changing media authorization, selection, mutation, or storage authority; representative-device/adaptive-layout acceptance remains open.

**Record type:** Repository planned/incomplete-feature inventory  
**Repository:** `GoreeCloud/gallery`  
**Lifecycle:** Development / non-Stable  
**Authority:** Current `main` source, accepted repository evidence, and active GoreeCloud Tasks Management obligations  
**Governing standard:** Standard — Repository Feature Tracking and Changelog Governance v1.0, effective September 22, 2026.

## Interpretation

Items here are planned, incomplete, blocked, or still acceptance-gated. Their presence does not imply implementation or release readiness. Where a partial foundation already exists, the verified implemented portion is also described in `IMPLEMENTED-FEATURES.md`.

## Current stabilization obligations

- Execute representative physical-device validation for Recycle Bin browsing, Restore/Purge, ordinary Trash/permanent-delete mode, cancellation, mixed media, partial-media access, permission revocation, restart/process recreation, provider failure, OEM/profile behavior, and retention/expiry refresh.
- Complete destructive-operation acceptance and confirm bounded post-mutation refresh behavior.
- Refine multi-select from physical-device and accessibility evidence and add only actions backed by accepted authority.
- Complete GLAZE UI V1.6 rendered, accessibility, adaptive-layout, performance, rollback, and Human Visual Excellence acceptance for Gallery.
- Complete TalkBack, switch access, large-text, contrast, reduced-motion/transparency, tablet/foldable, and representative-device acceptance.
- Complete signed release packaging, upgrade/recovery validation, Release Candidate evidence, production acceptance, and Stable qualification.
- Complete applicable Privacy Shield, Wardveil Security, Everkeep, GoreeCloud Identity, GoreeCloud Mesh, GoreeCloud Manager, GoreeCloud Policy, and GoreeCloud Observability evaluation/integration with evidence-backed dispositions.

## Product capability work still required

- Richer timeline-oriented browsing beyond the Day / Month / Year / None grouping and persisted Newest/Oldest ordering candidate, broader layout controls, and representative-device/adaptive-layout acceptance.
- Complete acceptance for the implemented app-layer Move path: representative physical-device/OEM/profile testing of existing-folder and `Create & move` flows, Android confirmation approve/cancel/deny behavior, Activity recreation while authorization is pending, mixed-media destination roots, provider failure, partial/revoked media access, concurrent/stale selection handling, and post-move refresh/result behavior.
- Album creation beyond the bounded Move-new-folder workflow, album rename/reorder, and approved Copy organization through Android-supported media boundaries.
- Expand and accept the implemented orientation-aware viewport-bounded image viewer beyond its current 2048px long-edge decode ceiling only where product need justifies true full-resolution/zoom behavior; representative-device quality, memory, accessibility, and form-factor acceptance remain open.
- Native video playback and activation of the persisted autoplay/loop preferences only after acceptance.
- Animated GIF thumbnail decoding before treating the persisted GIF preference as active behavior.
- Complete acceptance and expansion of the bounded first-party photo editor beyond its current rotate/flip/crop/save-copy foundation, including representative-device accessibility, large-image/memory behavior, failure/recovery, and approved metadata-editing workflows.
- Expand and complete acceptance for the implemented bounded photo-only slideshow: representative physical-device presentation, large-text/top-bar fit, TalkBack/Switch Access, orientation/form-factor behavior, optional presentation controls, and any separately justified video/presentation behavior.
- Expanded contextual/overflow actions and broader Share/export acceptance where required.
- Secure Private/Protected Photos and hidden/excluded-media policy using supported Android/GoreeCloud authentication, authorization, and protected-storage mechanisms.
- Safe empty-folder cleanup before activating the persisted cleanup preference.
- Additional mature Gallery capabilities evidenced by accepted historical GoreeCloud Gallery behavior, without copying proprietary Samsung source/assets/trademarks/implementation details.

## Product direction

- Optional user-controlled GoreeCloud Photos integration behind explicit adapters.
- Richer local organization/search experiences that remain device-local by default.
- Everkeep-governed continuity and recovery.
- Wardveil-governed security-sensitive media workflows.

## Explicit non-claims

Until the corresponding acceptance evidence exists, this file does not claim:

- production-safe destructive media operations across supported devices/OEMs/profiles;
- complete native playback or editing;
- accepted protected/private media storage;
- complete Integral Platform System conformance;
- production signing/distribution;
- Release Candidate, Production Acceptance, or Stable status.

## Maintenance rule

Move an item to `IMPLEMENTED-FEATURES.md` only after the authoritative implementation and required verification are integrated. Record material lifecycle changes in `CHANGELOGS.md`. Keep actionable execution work in GoreeCloud Tasks Management without creating duplicate task authority.