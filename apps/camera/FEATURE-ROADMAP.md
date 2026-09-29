# GoreeCloud Camera — Feature Roadmap

> Repository document version: **0.5.0**  
> Product internal version: **0.1.0**  
> Release lifecycle: **Concept**  
> Drive synchronized representation: **GoreeCloud/Feature Roadmap/GoreeCloud Camera/FEATURE-ROADMAP.md**

The repository `FEATURE-ROADMAP.md` is the **canonical editable roadmap source** under current GoreeCloud governance. The corresponding Drive Markdown record is the synchronized GoreeCloud-wide representation. Both must remain materially consistent with authoritative project documentation and verified implementation evidence.

Items remain **planned unless explicitly reclassified by verified implementation evidence**. The native Android foundation is currently in progress as recorded below; this does not promote the product beyond Concept.

## Product direction

GoreeCloud Camera is planned as one native application combining **Professional Camera**, **Creative Camera**, and **Private Camera**. Core capture must remain offline-first, local, user-owned, and independent of mandatory accounts or cloud services.

## Delivery sequence

### Phase 0 — Governance and architecture baseline
Status: **Substantially complete; licensing blocker remains**

Completed repository work includes the canonical Drive project specification, synchronized Drive roadmap, repository documentation baseline, product version identity, Platform Contract 0.2 declaration, current lifecycle classification, and exact-revision Platform Contract validation. A recognized repository license remains unresolved.

### Phase 1 — Native Android capture foundation
Status: **In progress**

Verified on authoritative `main` through video/audio source-foundation commit `f6d414049f6ad4792a70da00903fead403dde58c`:

- real Android/Gradle project;
- canonical app identity and version metadata;
- Android 17/API 37 compile and target baseline;
- provisional API 29 minimum SDK;
- camera permission flow;
- lifecycle-owned viewfinder host;
- camera enumeration and Capability Registry;
- deterministic default-camera selection;
- Camera2 preview session controller;
- representative Android 16/API 36 emulator qualification for preview and bounded JPEG capture;
- bounded JPEG Photo + MediaStore implementation;
- bounded video output discovery and Camera2/MediaRecorder recording-session source implementation;
- H.264 MP4 video plus AAC microphone audio source implementation;
- explicit just-in-time microphone authorization and microphone hardware gating;
- deterministic UTC JPEG and MP4 naming;
- pending MediaStore photo/video publication and handled-failure cleanup;
- source/static checks, unit tests, lint, APK assembly/provenance, and Platform Contract validation;
- Platform Contract 0.2 with all application-specific runtime Platform System integrations still blocked/unverified.

PR #4 candidate `cdc59289c607558090c298ee74ae2829268920c1` passed Android Foundation run `35020668795`, including preview-runtime emulator qualification, and Platform Contract run `35020668711`. After merge, authoritative commit `6dbd2c1a5c5e3fb523662f5a6e181c6dbf73644a` passed Android Foundation run `35021425774` and Platform Contract run `35021428000`. Documentation reconciliation PR #5 then produced authoritative `main` commit `1687d81a65c037b46a25f98a23e5a0d1453e58de`, whose Android Foundation and Platform Contract gates also passed.

PR #6 final candidate `a378a17d3e6cf28a16521428fa3d77c1910e2e53` passed Android Foundation run `35083327911`, including the Android 16 / API 36 `capture-runtime-emulator` job, and Platform Contract run `35083328806`. PR #6 was squash-merged as signed authoritative `main` commit `d5272da9877b47bfca1551784f32b1031a084a8e`. Push-triggered Android Foundation run `35083832517` then passed both ordinary validation/build and the same JPEG + MediaStore runtime gate on the merged revision, while Platform Contract run `35083833021` also passed.

PR #8 final candidate `86d2d63f373ddbe7df8e6681925fafb6596972f6` passed Android Foundation run `35101834080`, including source-contract checks, lint, unit tests, APK assembly, Concept-stage APK evidence, and the existing Android 16/API 36 preview/JPEG regression. Platform Contract run `35101835061` also passed. PR #8 was squash-merged as signed authoritative `main` commit `f6d414049f6ad4792a70da00903fead403dde58c`. Push-triggered Platform Contract run `35102551620` passed on that merged revision; its Android Foundation validation/build job also passed while the preview/JPEG emulator regression remained a separate gate during documentation reconciliation.

The representative emulator evidence proves that the current authoritative Camera2 path can open/configure a repeating preview and complete one bounded JPEG still capture through MediaStore on the configured Android virtual camera. It does **not** qualify a physical phone, prove OEM/device support, establish production UI acceptance, prove video/audio runtime behavior, prove microphone routing, or establish release readiness.

#### PR #6 — first bounded JPEG Photo + MediaStore implementation

PR #6 is implemented and verified on authoritative `main` while preserving product version `0.1.0`, Concept lifecycle, Platform Contract 0.2, and the single Camera2 session authority.

Verified implementation includes:

- JPEG output-size discovery through the Capability Registry;
- one JPEG `ImageReader` added to the existing Camera2 session outputs;
- explicit engineering-stage **Capture photo** control and `CAPTURING` state;
- bottom-system-bar inset handling that keeps the engineering shutter clear of system navigation UI;
- one-shot `TEMPLATE_STILL_CAPTURE` requests;
- deterministic UTC `GCAM_*.jpg` naming;
- MediaStore reservation in `DCIM/GoreeCloud Camera` using `IS_PENDING=1`;
- JPEG signature validation before publication;
- publish-after-write by clearing `IS_PENDING`;
- deletion of the pending MediaStore row on handled reserve/capture/write/finalization failure paths;
- lifecycle cleanup of pending capture state and `ImageReader` resources;
- source-contract validation, filename unit coverage, and Android 16/API 36 emulator qualification that verified a non-zero published JPEG and JPEG signature.

The runtime result is bounded representative-emulator evidence, not physical-device or production acceptance.

#### PR #8 — bounded video/audio source and build foundation

PR #8 is implemented on authoritative `main` while preserving product version `0.1.0`, Concept lifecycle, Platform Contract 0.2, and the existing Camera2 session authority.

Verified source/build implementation includes:

- bounded MediaRecorder-compatible video-size discovery through the Capability Registry;
- Camera2 `TEMPLATE_RECORD` sessions with preview and recorder outputs;
- MP4 output using H.264 video;
- AAC microphone audio using mono 48 kHz / 128 kbps encoding;
- bounded 30 fps video with current engineering bit-rate selection;
- deterministic UTC `GCAM_*.mp4` naming;
- pending MediaStore video reservation under `DCIM/GoreeCloud Camera`;
- non-zero-size validation before video publication;
- handled-failure pending-row discard;
- microphone hardware gating;
- `RECORD_AUDIO` requested only after a deliberate Record-video action;
- no automatic recording after microphone permission grant;
- explicit recording, stopping, microphone-active, saved, and failure states in the engineering shell;
- lifecycle cleanup and preview restoration around recording;
- no Internet, location, broad storage, all-files, or media-library read authority;
- source-contract validation and deterministic MP4 filename unit coverage;
- exact-head lint, unit-test, APK-assembly, provenance, and Platform Contract evidence.

This milestone is **not** runtime video/audio qualification. The existing CI emulator runtime lane exercises preview/JPEG behavior and does not establish MediaRecorder video operation, microphone routing, audio/video synchronization, physical-device recording support, or production acceptance.

Still required to complete Phase 1:

- explicit video/audio runtime qualification on an audio-capable representative target;
- initial physical-device preview/photo/video support and camera-quirk qualification evidence;
- integrate and validate the stacked local-settings candidate carrying persisted composition-grid and volume-key shutter preferences;
- stronger interrupted/process-death photo and recording recovery beyond handled in-process cleanup;
- resolution of the public-repository open-source license blocker.

Camera remains **Concept**. Source/build success and representative emulator runtime evidence materially strengthen implementation confidence but do not by themselves establish a user-ready release or lifecycle promotion.

The stacked settings continuation now carries two bounded local controls: a default-off app-private composition-grid preference with a non-interactive rule-of-thirds overlay, and a default-on volume-key shutter preference that reuses the existing photo action only from a ready PREVIEWING session. Modal surfaces, repeat key events, and non-preview states fail closed. The candidate does not add new capture, storage, permission, network, microphone, or cloud authority and does not close the broader local-settings milestone.

### Phase 2 — Capture reliability and device qualification
Status: **Planned**

- atomic/recoverable still and recording finalization beyond the bounded Phase 1 MediaStore commit paths;
- recoverable/journaled recording design;
- storage reserve/remaining-time logic;
- battery-critical finalization;
- thermal degradation policy;
- camera-service/lifecycle recovery;
- device capability profiles and known-quirk overrides;
- qualification evidence tied to app/device/OS revisions.

### Phase 3 — High-quality automatic photography
Status: **Planned**

Exposure/white-balance optimization, dynamic-range enhancement, local multi-frame processing, low-light optimization, face-aware exposure, natural skin-tone handling, lens/distortion correction, and processing-quality profiles.

### Phase 4 — Advanced photography and filmmaking
Status: **Planned**

Night, Portrait, Motion Photos, burst/Best Shot, Pro Photo, RAW, Pro Video, Cinema, professional monitoring, stabilization/Horizon Lock, and qualified external microphone/storage workflows.

### Phase 5 — Multi-camera and creator workflows
Status: **Planned**

Dual Camera, separate-stream/combined recording, concurrent cameras, Director Mode, Multi-Capture, teleprompter, multi-clip creator tools, slow motion, timelapse, hyperlapse, panorama, macro, and product photography.

### Phase 6 — Creative platform and GoreeCloud Lenses
Status: **Planned**

Creative Mode, filters/styles, signed and sandboxed Lens packages, permission/resource controls, local effects, and future Lens Studio authoring direction.

### Phase 7 — Scanning and visual utilities
Status: **Planned**

Document scanning, multi-page capture, OCR/searchable documents, QR/barcode recognition, text/object/color/measurement assistance, and privacy-aware accessibility descriptions.

### Phase 8 — Private Camera, metadata, and provenance
Status: **Planned**

Private Capture, protected destinations, metadata policy, sharing-time privacy controls, provenance relationships, and future content-authenticity support.

### Phase 9 — GoreeCloud ecosystem integration
Status: **Planned**

Platform Contract 0.4 evaluation against exactly nine Integral Platform Systems: Manager, Privacy Shield, Wardveil Security, Everkeep, Glaze UI, Mesh, Identity, Policy, and Observability. GoreeCloud Sync remains separately governed. GLAZE UI migration must target current Official Stable V1.6 / 1.6.0 and remain repository-locally acceptance-gated; no application-specific Glaze acceptance is claimed by the current engineering shell.

### Phase 10 — Remote and adaptive hardware experiences
Status: **Planned**

Remote Viewfinder, explicit pairing/session authority, foldable and large-screen layouts, outer-screen preview, rear-camera selfies, tabletop/flex posture, and secondary displays where supported.

### Phase 11 — Release qualification
Status: **Planned**

Real-device matrix, recovery/security/privacy/accessibility acceptance, current Glaze UI conformance, platform-system evidence, packaging/release evidence, documentation synchronization, and production-readiness gates before Stable.

## Canonical feature families

The synchronized Drive roadmap retains the detailed capability requirements for automatic photography, low light, portraits, Motion Photos, Best Shot, professional still/video, Cinema, stabilization, dual/multi-camera, creative capture, Lenses, filters, creator tools, scanning, visual utilities, Camera Intelligence, Private Capture, metadata, offline-first architecture, Gallery/Photos/Everkeep relationships, Glaze UI, Wardveil Security, Privacy Shield, Everkeep, capability profiles, reliability, storage/thermal/battery safeguards, provenance, accessibility, diagnostics, qualification, and release acceptance.
