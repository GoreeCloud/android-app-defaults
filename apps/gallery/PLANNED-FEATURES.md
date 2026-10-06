# GoreeCloud Gallery — Planned Features and Open Obligations

## October 6, 2026 — viewer context and hardware-control acceptance candidate

The current candidate extends the authorized viewer with bounded album/Favorites navigation, image-only Set photo as handoff, and hardware navigation/playback/Favorite/close controls. Exact-head protected build/runtime validation plus representative physical keyboard/D-pad, TalkBack/Switch Access, OEM chooser/Set-as behavior, partial-access revocation, and viewer lifecycle acceptance remain required before integration or broader maturity claims.


## October 3, 2026 — GoreeCloud OS Mobile default-gallery acceptance

The current application line now has the bounded Android entry points needed for GoreeCloud OS Mobile to select `com.goreecloud.gallery` as its system Gallery package. Remaining acceptance is distribution- and device-specific: verify the Android `SYSTEM_GALLERY` role on the physical `dre` qualification device, image/video VIEW and REVIEW routing, secure-review behavior, permission grant/denial/revocation, user/profile isolation, and rollback to the prior Lineage gallery path. The OS packaging layer must remain responsible for role/default configuration; the Gallery APK must not silently seize default-app authority when installed on an ordinary Android system.

## October 2, 2026 — bounded selection overflow integrated behavior

Current authoritative source exposes **Select all visible / Clear selection** through the multi-select More menu without another persistent action button. Selection remains bounded to the current Android-authorized and presented media scope through `GallerySelectionPolicy.selectAll`; stale or foreign content URIs cannot become selection authority. Details remains available only for a one-item selection. Focused policy coverage verifies partial, complete, empty, and stale-selection states. Representative-device/accessibility acceptance remains open.

## October 2, 2026 — slideshow controls integration and repeat candidate

Protected PR #185 integrated explicit **Pause / Resume** controls for the existing local photo-only slideshow to authoritative monorepo main `7902e7bd58a510591e6c8f8778a3872df4c77642`. Pausing preserves viewer position while lifecycle exit and manual navigation continue to terminate slideshow state. No media authority, permission, storage mutation, network path, or video behavior was expanded.

Current authoritative source also includes a session-local **Loop** control. At the end of a slideshow, repeat wraps to the first eligible photo only when at least two photos are available; a one-photo collection fails closed instead of spinning indefinitely. Repeat does not alter persisted Gallery settings or media state. Representative-device presentation/accessibility/form-factor acceptance remains open.

## September 29, 2026 — timeline grouping continuation

The current candidate advances the richer-browsing backlog with local **Day / Month / Year / None** grouping plus a persisted **Newest first / Oldest first** presentation preference over the same authorized media snapshot. Day and Newest first remain migration-safe defaults. Settings portability includes grouping and sort order, and the same continuation restores the previously exported-but-not-imported `viewDensity` value. Broader timeline navigation, additional layout controls, and representative-device/adaptive-layout acceptance remain open. Fresh exact-head validation is required.

## September 28, 2026 — bounded slideshow candidate

PR #100 now implements a bounded local photo-only slideshow in the authorized viewer. It advances forward through later images, skips non-photo media, stops at the end, and cancels on lifecycle exit or manual navigation without expanding media authority. Exact candidate head `c30ba640e86f9e4b28f78f9ad7e8b80644eec00f` passed all configured Gallery workflow families. Representative-device/accessibility/form-factor acceptance and broader presentation actions remain open.


## 2026-09-28 candidate continuation

PR #100 now includes a persisted Dense/Comfortable/Spacious media-grid presentation control. The setting is included in Gallery settings import/export and has pure policy tests. It advances the view-density backlog without changing media authorization, selection, mutation, or storage authority; representative-device/adaptive-layout acceptance remains open.

**Record type:** Repository planned/incomplete-feature inventory  
**Repository:** `GoreeCloud/android-app-defaults` (`apps/gallery/`)  
**Lifecycle:** Development / non-Stable  
**Authority:** Current `main` source, accepted repository evidence, and active GoreeCloud Tasks Management obligations  
**Governing standard:** Standard — Repository Feature Tracking and Changelog Governance v1.0, effective September 22, 2026.

## Interpretation

Items here are planned, incomplete, blocked, or still acceptance-gated. Their presence does not imply implementation or release readiness. Where a partial foundation already exists, the verified implemented portion is also described in `IMPLEMENTED-FEATURES.md`.

## Current stabilization obligations

- Execute representative physical-device validation for Recycle Bin browsing, Restore/Purge, ordinary Trash/permanent-delete mode, cancellation, mixed media, partial-media access, permission revocation, restart/process recreation, provider failure, OEM/profile behavior, and retention/expiry refresh.
- Complete destructive-operation acceptance and confirm bounded post-mutation refresh behavior.
- Refine multi-select from physical-device and accessibility evidence and add only actions backed by accepted authority.
- Complete Glaze V1.7 rendered, accessibility, adaptive-layout, performance, rollback, and Human Visual Excellence acceptance for Gallery.
- Complete TalkBack, switch access, large-text, contrast, reduced-motion/transparency, tablet/foldable, and representative-device acceptance.
- Complete signed release packaging, upgrade/recovery validation, Release Candidate evidence, production acceptance, and Stable qualification.
- Complete applicable Privacy Shield, Wardveil Security, Everkeep, GoreeCloud Identity, GoreeCloud Mesh, GoreeCloud Manager, GoreeCloud Policy, and GoreeCloud Observability evaluation/integration with evidence-backed dispositions.

## Product capability work still required

- Richer timeline-oriented browsing beyond the Day / Month / Year / None grouping and persisted Newest/Oldest ordering candidate, broader layout controls, and representative-device/adaptive-layout acceptance.
- Complete acceptance for the implemented app-layer Move path: representative physical-device/OEM/profile testing of existing-folder and `Create & move` flows, Android confirmation approve/cancel/deny behavior, Activity recreation while authorization is pending, mixed-media destination roots, provider failure, partial/revoked media access, concurrent/stale selection handling, and post-move refresh/result behavior.
- Complete acceptance for preserve-original Copy: representative physical-device/OEM/profile testing of existing-folder and `Create & copy` flows, concrete internal/removable MediaStore volume behavior, related-URI insert handling, mixed source folders/media types, duplicate/long-name planning, large media, partial provider/source/output failures, storage exhaustion, lifecycle interruption, post-copy refresh, metadata/orientation fidelity, accessibility, performance/power, and current Glaze conformance.
- Album creation beyond the bounded Move-new-folder and Copy-new-folder workflows; complete acceptance/refinement for the active bounded album-rename candidate across Android confirmation approve/cancel/deny, Activity recreation, full-vs-partial media access, root/nested folder behavior, large albums at the 250-item mutation bound, internal/removable volumes, provider failures, partial execution, collisions, refresh, accessibility, performance/power, and current Glaze presentation; continue broader provider-backed organization only where Android exposes safe authority; and complete acceptance/refinement of the implemented app-local Pin/Unpin plus Move earlier / Move later ordering.
- Complete representative-device/OEM/profile, large-image, orientation, large-text, TalkBack/Switch Access, gesture-conflict, performance, memory, and form-factor acceptance for the implemented 1×–4× bounded viewer zoom/pan path. The underlying viewer decode remains viewport-bounded with a 2048px long-edge ceiling; expand beyond that ceiling only where product need separately justifies true original/full-resolution zoom behavior.
- Complete representative-device, accessibility, form-factor, failure/recovery, and release acceptance for the implemented native video playback and active autoplay/loop preferences; expand playback controls only where product evidence justifies it.
- Complete acceptance for the implemented opt-in animated GIF thumbnail path: representative physical devices/OEMs/profiles, large and complex GIFs, decode/provider failure, scroll/detach lifecycle, TalkBack/Switch Access, reduced-motion expectations, performance/power, and current Glaze conformance.
- Complete acceptance and expansion of the bounded first-party photo editor beyond its current rotate/flip/crop/save-copy foundation, including representative-device accessibility, large-image/memory behavior, failure/recovery, and approved metadata-editing workflows.
- Expand and complete acceptance for the implemented bounded photo-only slideshow and its persisted 3/5/10-second pace control: representative physical-device presentation, large-text/top-bar fit, TalkBack/Switch Access, orientation/form-factor behavior, optional presentation controls, and any separately justified video/presentation behavior.
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