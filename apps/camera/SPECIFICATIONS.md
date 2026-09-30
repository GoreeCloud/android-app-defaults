# GoreeCloud Camera — Repository Specifications

> Repository document version: **0.5.0**  
> Product internal version: **0.1.0**  
> Release lifecycle: **Concept**  
> Repository: `GoreeCloud/goreecloud-camera`

## 1. Purpose and authority

GoreeCloud Camera is the GoreeCloud-owned mobile capture application for still photography, video, scanning, creative capture, computational imaging, visual utilities, and privacy-controlled acquisition of source media.

This repository contains the **native Android implementation foundation**, representative Android-emulator qualification for Camera2 preview plus one bounded JPEG + MediaStore still-capture path, and the first bounded video/audio **source and build** foundation merged through PR #8. The current video/audio source uses Camera2 and `MediaRecorder` for MP4/H.264 video with AAC microphone audio, deterministic UTC naming, pending MediaStore publication/cleanup, explicit recording state, microphone hardware gating, and just-in-time microphone permission. It does **not** yet establish video/audio runtime qualification, microphone-routing qualification, physical-device support, production UI acceptance, robust process-death recording recovery, or accepted GoreeCloud Platform System integration.

The release lifecycle remains **Concept**. Source implementation and representative runtime evidence are necessary implementation evidence, but neither is by itself a lifecycle promotion. Promotion must be evaluated separately against the authoritative lifecycle standard and the actual maturity of usable capture, persistence, device support, recovery, privacy/security, accessibility, and acceptance evidence.

The canonical Drive project specification is `GoreeCloud/Projects/Project Specification — Camera.md`. The repository `FEATURE-ROADMAP.md` is the canonical editable roadmap source; `GoreeCloud/Feature Roadmap/GoreeCloud Camera/FEATURE-ROADMAP.md` is its synchronized Drive representation. This file is the repository-coupled technical specification and must remain materially synchronized with the Drive project specification and roadmap state.

## 2. Verified current implementation state

Authoritative `main` currently establishes the following verified foundation:

- A native Android application module and Gradle build exist.
- Canonical Android application ID and namespace: `com.goreecloud.camera`.
- Product version: `0.1.0`; version code: `1`.
- Release lifecycle remains `Concept`.
- Strategic compile/target API: Android 17 / API 37.
- Provisional minimum API: 29. This is an engineering baseline, not a final support-policy commitment.
- Primary language: Kotlin using Android Gradle Plugin built-in Kotlin support.
- Android Gradle Plugin: 9.4.0; CI Gradle: 9.6.0; Java toolchain compatibility: 17.
- Runtime manifest declares `android.permission.CAMERA` and `android.permission.RECORD_AUDIO`.
- Camera permission is required for preview/capture. Microphone permission is requested just in time only after a deliberate Record-video action, and granting it does not automatically start recording.
- No Internet, location, broad storage, all-files, or media-library read permission is requested.
- A Camera2 session controller owns camera opening, preview/still/recording-session creation, repeating requests, stop, and shutdown behavior.
- A Capability Registry enumerates cameras, lens direction, hardware level, RAW capability, logical multi-camera capability, preview/JPEG output sizes, and bounded MediaRecorder-compatible video sizes.
- A deterministic selector prefers back, external, front, then unknown-facing cameras.
- Initial unit tests cover deterministic camera selection plus deterministic photo/video filename generation.
- Static source-contract validation and Android CI are defined for exact candidate revisions.
- Pull request #2 was squash-merged to authoritative `main` as signed commit `8de1eac6693f09ef52b0f12886108727352404e1`, establishing the native Android source foundation.
- Pull request #4, **“Qualify Camera2 preview on Android emulator,”** was squash-merged to authoritative `main` as signed commit `6dbd2c1a5c5e3fb523662f5a6e181c6dbf73644a`.
- Exact PR #4 candidate `cdc59289c607558090c298ee74ae2829268920c1` passed Android Foundation run `35020668795`, including the Android 16 virtual-camera preview qualification job, and Platform Contract run `35020668711`.
- Authoritative merge commit `6dbd2c1a5c5e3fb523662f5a6e181c6dbf73644a` passed push-triggered Android Foundation run `35021425774`, including the same runtime preview qualification, and Platform Contract run `35021428000`.
- Documentation reconciliation PR #5 produced authoritative `main` commit `1687d81a65c037b46a25f98a23e5a0d1453e58de`, whose Android Foundation and Platform Contract workflows also passed.
- The representative Android 16/API 36 emulator uses an emulated back camera and has reached `Session: previewing` with one detected virtual camera.
- Runtime preview evidence includes UI hierarchy, screenshot, CameraService state, package/app-ops state, logcat, and provenance; it explicitly records `physical_device_qualification=false` and no Stable release authority.
- PR #6 final candidate `a378a17d3e6cf28a16521428fa3d77c1910e2e53` passed Android Foundation run `35083327911`, including the Android 16/API 36 JPEG + MediaStore `capture-runtime-emulator` job, and Platform Contract run `35083328806`.
- PR #6 was squash-merged as signed authoritative `main` commit `d5272da9877b47bfca1551784f32b1031a084a8e`. Push-triggered Android Foundation run `35083832517` passed ordinary validation/build plus the JPEG + MediaStore runtime gate on that exact merged revision, and Platform Contract run `35083833021` passed.
- PR #7 reconciled repository implementation records and was squash-merged as signed authoritative `main` commit `5017457a2e61716cf4eed6a0cae87922842eefc9`; post-merge Android Foundation run `35096881148` and Platform Contract run `35096881861` passed, including representative preview/JPEG runtime replay.
- PR #8, **“Implement bounded Camera video/audio capture foundation,”** final candidate `86d2d63f373ddbe7df8e6681925fafb6596972f6` passed Android Foundation run `35101834080` and Platform Contract run `35101835061`. Its Android Foundation validation covered source-contract checks, lint, unit tests, debug APK assembly, Concept-stage APK evidence, and the existing Android 16/API 36 preview/JPEG regression job.
- PR #8 was squash-merged as GitHub-signed authoritative `main` commit `f6d414049f6ad4792a70da00903fead403dde58c`. Push-triggered Platform Contract run `35102551620` passed and Android Foundation run `35102550065` passed, including ordinary validation/build plus the same representative preview/JPEG regression.

PR #6 implements and verifies on representative Android emulator:

- JPEG output-size discovery in the Capability Registry;
- one JPEG `ImageReader` attached to the same Camera2 capture-session authority as preview;
- explicit `CAPTURING` state and engineering-stage **Capture photo** control;
- bottom-system-bar inset handling that keeps the shutter clear of system navigation UI;
- one-shot `TEMPLATE_STILL_CAPTURE` requests;
- deterministic UTC `GCAM_*.jpg` naming;
- MediaStore reservation under `DCIM/GoreeCloud Camera` using `IS_PENDING=1`;
- JPEG signature validation before publication;
- write-then-publish behavior that clears `IS_PENDING` only after successful output completion;
- deletion of the pending row on handled reserve/capture/write/finalization failures;
- cleanup of `ImageReader`, pending-photo state, and in-flight capture state through the existing session lifecycle;
- representative Android 16/API 36 runtime qualification that triggered one photo, located the published non-zero MediaStore row, read the output, and verified its JPEG signature.

PR #8 implements in authoritative source/build:

- MediaRecorder-compatible video-size discovery in the Capability Registry;
- capability gating for video recording and microphone hardware availability;
- explicit `STARTING_VIDEO`, `RECORDING`, and `STOPPING_VIDEO` session states;
- an engineering Record-video control with truthful microphone/recording status;
- just-in-time `RECORD_AUDIO` permission request after deliberate recording intent, without auto-start after permission grant;
- Camera2 `TEMPLATE_RECORD` with preview and MediaRecorder surfaces under the existing single session authority;
- MP4/H.264 video configuration plus AAC microphone audio;
- deterministic UTC `GCAM_*.mp4` naming;
- pending MediaStore video reservation/publication and handled-failure discard cleanup;
- recording stop/finalization, recorder release, session cleanup, and preview restoration;
- source-contract checks and deterministic video filename unit coverage.

The PR #8 Android emulator gate re-qualified the existing preview/JPEG behavior only. It did **not** exercise MediaRecorder recording, AAC audio capture, microphone routing, or MP4 publication. Therefore the following remain unverified or incomplete:

- successful preview/still/video qualification on a representative physical phone;
- video/audio runtime qualification on an audio-capable representative target;
- microphone-routing and captured-audio-track verification;
- camera switching and production user-facing mode controls;
- physical-device qualification or Camera-specific hardware profiles/quirk records;
- broader local settings beyond the stacked composition-grid and volume-key shutter candidate;
- robust interrupted/process-death still/video recovery beyond handled in-process cleanup;
- production signing or packaged release;
- accepted Glaze UI, Privacy Shield, Wardveil Security, Everkeep, Manager, Mesh, or Identity runtime integration;
- a recognized open-source repository license.

Earlier runtime attempts exposed a case-sensitive shutter-selector defect, one Quickstep emulator ANR, and an engineering-shutter/system-taskbar overlap. Those failures did not establish a Camera2/MediaStore defect; the overlap was corrected with bottom system-bar inset handling, the runtime selector was constrained to the Camera package, and later exact candidates plus authoritative merged revisions subsequently passed.

### 2.1 Stacked local-settings candidate

The current unmerged settings continuation adds two app-private preferences. **Composition grid** defaults off, persists across ordinary restart, and renders a non-interactive rule-of-thirds overlay above the live preview and below Camera controls. **Volume keys capture photo** defaults on and maps Volume Up/Down to the existing still-capture action only for the first key-down while the single Camera session is PREVIEWING and no Camera modal is visible; held/repeated presses and non-preview states fail closed. Neither preference adds a second capture path, changes MediaStore publication, requests permissions, broadens microphone/network/account authority, or changes lifecycle classification. Source/runtime validation on the exact candidate head remains required before this slice is treated as verified Development evidence.

## 3. Product scope and boundaries

Camera owns capture-session orchestration and production of source media. It must not become the authoritative media-library, general backup, synchronization, or long-term organizational system.

Planned authority boundaries:

- **GoreeCloud Camera** — capture.
- **GoreeCloud Gallery** — local media viewing and management.
- **GoreeCloud Photos** — backup, synchronization, organization, intelligence, sharing, and preservation.
- **Everkeep** — resilience, preservation, portability, continuity, succession, and recovery requirements.

Extensions, scanners, remote-control clients, and downstream apps must not acquire broader camera or media authority merely because they participate in a capture workflow.

## 4. Platform and technology direction

The first implementation is a native Android application using Kotlin and standard Android/Gradle organization.

### 4.1 Current build baseline

- `compileSdk = 37`
- `targetSdk = 37`
- `minSdk = 29` — provisional engineering minimum
- Android Gradle Plugin `9.4.0`
- Gradle `9.6.0` in CI
- Java source/target compatibility `17`
- Android application ID `com.goreecloud.camera`

The strategic platform target follows GoreeCloud OS Mobile: Android 17 / API 37. Android 16/API 36 emulator qualification is representative runtime evidence only; the physical GoreeCloud OS Mobile qualification baseline remains separately governed and does not automatically qualify Camera.

### 4.2 Camera framework strategy

The implementation uses direct **Camera2** APIs for preview, capability discovery, JPEG still capture, and recording-session coordination. The current bounded recording source uses Android `MediaRecorder` for MP4/H.264 video and AAC microphone audio. This preserves a clear path to advanced manual, RAW, concurrent-camera, high-speed, and device-specific behavior while keeping one explicit Camera2 session authority. Future work may introduce higher-level Android camera abstractions where they materially improve lifecycle handling or compatibility, but they must not obscure verified hardware capability or create a second conflicting session authority.

### 4.3 User-interface direction

The current activity is an **engineering capture shell**, not an accepted GoreeCloud Camera user experience. The production UI must use the current approved Stable GoreeCloud Glaze UI contract. Current Official Stable consumer authority is **GLAZE UI V1.6 / 1.6.0** at accepted release source `a7180679ea851389e0f3004515f9a25f420e716d`. No repository-local Glaze implementation or application conformance is claimed by the current engineering shell.

Unsupported hardware capabilities must remain hidden or unavailable rather than being advertised from theoretical API support.

## 5. Architectural principles

### 5.1 Authorization travels with the operation

Each capture request should carry an effective context containing mode, requested camera set, destination policy, Privacy Shield context, metadata policy, extension authority, remote-control authority, quality profile, and applicable security state. Downstream modules consume that context rather than re-deriving authority from identity alone.

The current bounded local capture foundation does not yet construct the full GoreeCloud capture authorization context because it has no network, account, extensions, remote control, location, or downstream-service invocation. Microphone authority is now materially necessary only for explicit video-with-audio recording, is requested just in time, and must not broaden unrelated preview/photo authority. These narrow Android permission checks do not substitute for the future operation-bound privacy/security contract required before more sensitive or connected capabilities appear.

### 5.2 Capability-driven hardware behavior

Camera must not assume uniform Android camera hardware. The current Capability Registry reads Camera2 characteristics and exposes source-level capability facts, including preview/JPEG sizes and a bounded MediaRecorder-compatible video size. A later effective device profile must combine reported capabilities with real-device qualification, including explicit workarounds or blacklists when evidence contradicts nominal capability reporting.

### 5.3 Single camera-session authority

`CameraSessionController` is the current owner of Camera2 device/session resources. Still-photo and bounded recording paths use this same authority rather than opening independent cameras. Activities, future modes, extensions, and platform adapters must not open independent camera sessions behind this authority. Session ownership should evolve behind narrow interfaces as additional capture engines are introduced.

### 5.4 Recoverable media lifecycle

The full capture pipeline must validate camera/storage/thermal/battery/microphone/permission prerequisites, run ephemeral preview analysis, stage incomplete output recoverably, apply privacy/metadata policy, finalize media atomically or recoverably, verify committed artifacts, notify only authorized downstream consumers, and remove temporary state after successful commit.

PR #6 implements the first bounded still portion of this model. PR #8 extends the same pending-media approach to a bounded video destination and performs handled-failure discard plus in-process recorder/session cleanup. Neither milestone is yet a complete crash/process-death journal or production-grade recovery system.

## 6. Major components and implementation status

- **Camera Session Controller — Foundation implemented; preview/still representative-emulator qualified; video source implemented.** Owns Camera2 lifecycle, preview/still/recording session resources, MediaRecorder lifecycle, and shutdown/preview restoration. Video runtime and physical-device qualification remain open.
- **Capability Registry — Foundation implemented.** Enumerates Camera2 capability facts and preview/JPEG/video output sizes; actual video runtime support remains qualification-dependent.
- **Preview Engine — Foundation implemented and representative-emulator qualified.** `TextureView` host plus Camera2 repeating preview request; physical-device and performance qualification remain pending.
- **Camera Selector — Foundation implemented.** Deterministic default-camera selection.
- **Photo Capture Engine — Bounded representative-emulator-qualified implementation.** One explicit JPEG still capture and handled-failure finalization exist; burst, RAW, computational pipelines, richer orientation/metadata controls, physical-device acceptance, and production photo UX remain planned.
- **Video Capture Engine — Bounded source/build foundation implemented; runtime unqualified.** Camera2/MediaRecorder MP4/H.264 + AAC recording, explicit recording states, MediaStore finalization/cleanup, microphone gating, and lifecycle restoration exist in source. Runtime recording, audio-route, physical-device, sustained-recording, interruption, and production qualification remain open.
- **Processing Pipeline — Planned.** Local computational photography and video transformations.
- **Metadata and Provenance Engine — Planned.** Metadata policy, artifact relationships, processing provenance, and future authenticity records.
- **Storage Coordinator — Partially implemented.** Pending MediaStore reservation/publish/discard exists for bounded JPEG and MP4 paths; broader storage reserve, protected destinations, journaling, process-death recovery, and richer cleanup remain planned.
- **Private Capture Controller — Planned.** Stricter metadata, analysis, backup, Lens, preview, sharing, and storage behavior.
- **Lens Runtime — Planned.** Signed, sandboxed, permission-scoped, resource-limited creative effects.
- **Scanner and Visual Utilities — Planned.** Document/code scanning and optional local visual assistance.
- **Remote Viewfinder Controller — Planned.** Explicit pairing, session-scoped authority, and visible remote control.
- **Platform Adapters — Planned.** Narrow integrations with applicable GoreeCloud systems.
- **Diagnostics — Planned.** Privacy-safe structured events and qualification evidence.

## 7. Functional requirements

The canonical Feature Roadmap defines detailed planned feature scope. Major families include:

- Photo, Video, Portrait, Night, Pro Photo, Pro Video, and Cinema modes.
- Burst, Motion Photos, Best Shot, RAW, stabilization, Horizon Lock, slow motion, timelapse, hyperlapse, panorama, macro, and product photography.
- Dual/multi-camera and Director Mode.
- Creative Mode, GoreeCloud Lenses, Lens Studio direction, filters, creator tools, and Multi-Capture.
- Document and code scanning plus privacy-aware visual utilities.
- Selfie, foldable/large-screen, and future Remote Viewfinder workflows.
- Local Camera Intelligence and shot guidance.
- Private Capture and metadata controls.
- Deep but non-mandatory Gallery/Photos/Everkeep integration.

The authoritative implementation currently provides the Android application shell, permission flow, capability discovery, camera selection, preview-session foundation, representative Android-emulator preview/JPEG qualification, and a bounded source/build video-with-audio foundation. Production Photo/Video modes and richer user-facing capture modes remain planned until their implementation and acceptance evidence reclassifies them.

## 8. Data and storage

Ordinary photographs and videos must remain normal user-owned files. GoreeCloud-specific relationships may use portable metadata or sidecars, but primary media must remain usable without GoreeCloud Camera.

PR #6's bounded still implementation creates a newly captured JPEG through Android MediaStore, reserving the row as pending under `DCIM/GoreeCloud Camera`, writing the JPEG, and publishing only after the write succeeds.

PR #8's bounded recording source similarly reserves a pending MediaStore MP4 destination, writes through its file descriptor using `MediaRecorder`, publishes after a successful stop/finalization path, and discards the pending item on handled failure. The current implementation does not read the user's existing media library, create an application database, persist preferences, create sidecars, or create capture relationship records.

Planned capture relationship data should be versioned and capable of representing primary media, RAW, motion sequences, separate camera streams, sidecars, integrity state, privacy policy, and computational-modification provenance.

Private Capture may use a protected destination, but export/recovery rules must remain explicit and user-controlled.

## 9. Interfaces and APIs

No public network API is implemented or required for core capture.

Current internal source interfaces include:

- `CameraDescriptor` and `LensFacing` for neutral camera identity/role representation;
- `CameraSelector` for deterministic default-camera selection;
- `CameraCapabilityRegistry` for Camera2 capability discovery plus preview/JPEG/video output-size selection;
- `CameraSessionController` for lifecycle-owned Camera2 preview, bounded still capture, and bounded recording resources;
- `CameraSessionState` for observable engineering-shell session state;
- `PhotoCaptureOutcome` for bounded still completion/failure reporting;
- `VideoRecordingOutcome` for bounded recording completion/failure reporting;
- `PhotoFileNamer` and `VideoFileNamer` for deterministic UTC media naming;
- `PhotoMediaStoreCommitter` / `PendingPhoto` for pending JPEG reservation, commit/publication, and discard cleanup;
- `VideoMediaStoreCommitter` / `PendingVideo` for pending MP4 reservation, publication, and discard cleanup.

Future platform adapters, capture-result contracts, Remote Viewfinder transport, Lens manifests, or integration APIs must be separately versioned when introduced. Core capture must never depend on a hosted control plane.

## 10. Authentication and authorization

Basic local capture must not require account authentication. Identity may become applicable to optional account-bound, paired-device, sharing, or cross-device workflows, but must not be used to broaden a capture operation beyond the authority carried by its capture context.

The current local preview/photo/video foundation has no account, network, remote-control, or Identity dependency.

Remote control requires explicit pairing, session-scoped authorization, visible active-state indication, and revocation when implemented.

## 11. Privacy requirements

### 11.1 Current implemented privacy boundary

The Android manifest declares:

- `android.permission.CAMERA`;
- `android.permission.RECORD_AUDIO`.

The current implementation intentionally does **not** request:

- `android.permission.INTERNET`;
- fine or coarse location;
- legacy storage permission;
- all-files access;
- media-library read permissions.

Camera permission gates preview/capture. Microphone permission is requested only after the user explicitly chooses to record video with audio. A microphone permission grant does not auto-start recording. The app also gates the recording control on actual microphone hardware availability.

The qualified preview/JPEG pipeline does not persist preview frames, upload content, record audio, read location, or access an existing media library. PR #8 adds source/build support for microphone-backed recording, but the representative emulator qualification still covers only preview/JPEG behavior and therefore does not prove microphone capture, routing, or audio-track correctness. These are implementation/evidence facts, not Privacy Shield acceptance evidence.

### 11.2 Future privacy requirements

- Core capture remains offline-first and local-first.
- Viewfinder/analysis frames are ephemeral by default.
- Location and other sensitive metadata are separately controllable.
- Private Capture provides a strong visible state and stricter destination/metadata/analysis/backup/extension behavior.
- Lens packages receive only minimum frame access and must not silently upload frames.
- Sharing/export supports Original, Privacy Safe, and Custom metadata policies when implemented.
- Optional external processing must not become mandatory for normal capture.
- Applicable Privacy Shield contracts fail closed for dependent features while unrelated local capture remains available where policy permits.

## 12. Security requirements

### 12.1 Current source-level controls

- Camera2 device and capture-session ownership is centralized in `CameraSessionController`.
- Camera access is gated by the Android camera runtime permission.
- Microphone use is gated by explicit recording intent, hardware capability, and Android microphone permission.
- Camera/session/surface/`ImageReader`/`MediaRecorder` resources are closed or released through lifecycle and handled-failure paths.
- Still output is held as a pending MediaStore item until a valid JPEG is written successfully.
- Video output is held as a pending MediaStore item until the bounded recorder stop/finalization path succeeds.
- Handled reserve/capture/record/write/finalization failures attempt to delete pending MediaStore items instead of intentionally publishing incomplete output.
- The application disallows cleartext network traffic and does not declare Internet permission.
- Android backup is disabled for this engineering foundation.
- Exact-revision qualification records package/app-ops and CameraService evidence without granting broader runtime authority than the tested operation requires.

These controls do not constitute Wardveil Security acceptance.

### 12.2 Future security requirements

- Protect temporary frames, RAW data, cached/in-progress video, Private Capture material, microphone use, location metadata, and remote-session state according to sensitivity and lifetime.
- Add process-death/interrupted-operation recovery so incomplete capture state cannot rely only on in-process cleanup.
- Lens packages must be signed, sandboxed, permission-scoped, resource-limited, and killable without destabilizing core capture where practical.
- Private Capture temporary data must not leak into ordinary thumbnails, logs, caches, or backup queues.
- Diagnostics must not contain raw frame/media payloads by default.
- Use mature approved cryptographic/platform primitives; do not invent custom cryptography.

## 13. GoreeCloud Platform Contract

The current candidate declares **Platform Contract 0.4** and exactly nine Integral Platform Systems. Current Official Stable GLAZE UI consumer authority is **V1.6 / 1.6.0**; the engineering shell remains `applicable-migration-required` for Glaze because it has no repository-local V1.6 implementation or application acceptance.

The nine systems are explicitly evaluated and remain blocked, migration-required, or otherwise unaccepted at this milestone:

1. GoreeCloud Manager
2. Privacy Shield
3. Wardveil Security
4. Everkeep
5. Glaze UI
6. GoreeCloud Mesh
7. GoreeCloud Identity
8. GoreeCloud Policy
9. GoreeCloud Observability

Camera preview/still/video source and emulator evidence is relevant product implementation evidence but does not satisfy any Platform System acceptance contract on its own. Policy and Observability are now explicitly evaluated rather than being omitted by the superseded seven-system model.

GoreeCloud Sync is separately governed and must not appear as a tenth `platform_systems` key. If Camera later uses Sync, synchronization authorization, datasets, version/change model, conflicts, replication, offline resume, and cross-device behavior must be documented separately.

## 14. Accessibility

The engineering shell provides basic text labels, explicit camera-permission control, a content description for the preview host, a stable description for the engineering shutter, and labeled recording/microphone state plus a video control. No accessibility acceptance is claimed.

Production requirements include meaningful labels/state, logical focus order, screen-reader announcements, non-color-only critical warnings, large-text and reduced-motion support, adequate contrast/target sizing, hardware/voice/gesture alternatives where appropriate, haptic confirmation, foldable/large-screen operability, and optional privacy-aware visual descriptions.

Accessibility acceptance requires real implementation and device validation.

## 15. Performance, reliability, and continuity

Performance budgets are qualification targets, not current claims. Real-device testing must establish thresholds for launch, preview readiness, shutter responsiveness, lens switching, sustained recording, memory/thermal behavior, storage exhaustion, battery-critical finalization, and interrupted-session recovery.

The current session controller explicitly closes resources across activity/surface lifecycle transitions, and representative emulator evidence verifies the preview/JPEG path. PR #8 adds bounded in-process MediaRecorder stop/failure cleanup and preview restoration in source, but it does not establish video runtime reliability, process-death recovery, sustained recording, microphone-route recovery, or physical-device capture reliability.

Correctness, media integrity, privacy, security, and thermal sustainability take precedence over headline latency.

Camera does not own general media backup/restore, but captured files must remain exportable and portable. In-progress capture/recording recovery is a Camera reliability responsibility once capture persistence exists.

## 16. Dependencies and configuration

The current implementation deliberately minimizes dependencies:

- Android platform/framework APIs for runtime behavior, including Camera2, MediaStore, and MediaRecorder;
- Android Gradle Plugin 9.4.0 as build tooling;
- Gradle 9.6.0 in CI;
- JUnit 4.13.2 for local unit testing;
- pinned `reactivecircus/android-emulator-runner` only as CI/runtime-qualification tooling, not as an application runtime dependency.

There is no third-party runtime camera library, hosted SDK, telemetry SDK, networking SDK, account SDK, or proprietary control-plane dependency in the application foundation.

Future dependencies must remain limited to justified open-source or platform foundations. Core operation must not depend on proprietary hosted control planes, vendor accounts, or commercial cloud processing.

Tracked configuration must contain no reusable secrets. User preferences, device qualification, privacy policy state, presets, extension permissions, diagnostics settings, and build-time configuration should remain clearly separated.

## 17. Observability

The current engineering shell exposes non-persistent session state, a coarse camera-count capability summary, bounded photo-saved/failure status, video capability state, microphone requirement state, and recording start/stop/finalization status. No telemetry or remote diagnostics are implemented.

CI runtime qualification captures local disposable-emulator evidence including UI hierarchy, screenshot, CameraService state, package/app-ops state, MediaStore query results, logcat, provenance, and—when the still-capture gate succeeds—the captured JPEG payload for signature verification. Current stacked PR #12 CI includes a separate Android 16 video/audio emulator lane that verifies an explicit microphone-permission flow, non-empty published MP4, H.264 video, AAC audio, non-trivial duration, and preview restoration. That evidence remains disposable-emulator Development/Concept evidence and does not qualify physical-device/OEM capture or microphone signal quality.

Future diagnostics may record privacy-safe structured events such as session lifecycle, mode, capability decisions, encoder configuration, thermal/storage state classes, error codes, recovery outcomes, and timing metrics.

Ordinary diagnostics must not contain raw camera frames, recognized faces, OCR content, precise location, microphone payloads, or captured user media. Test-only runtime evidence containing disposable synthetic media remains qualification material and must not be treated as normal product diagnostics.

## 18. Testing and validation

Current repository validation includes:

- static source-contract validation of application ID, API levels, product version, AGP pin, lifecycle declaration, required foundation/capture files, bounded permission authority, Camera2 JPEG still-capture primitives, MediaStore pending/finalization semantics, video-size discovery, MediaRecorder H.264/AAC configuration, just-in-time microphone request behavior, and forbidden Internet/location/broad-storage/media-read permissions;
- local pure-Kotlin unit tests for deterministic camera selection plus deterministic photo/video filename generation;
- Android lint, unit tests, and debug APK assembly in exact-revision CI;
- Platform Contract validation through the repository's pinned reusable contract workflow;
- authoritative preview/JPEG qualification on Android 16/API 36 with an emulated back camera;
- exact-revision still runtime qualification that installs the APK, grants camera authority, requires preview readiness, triggers one JPEG capture, verifies the matching non-zero MediaStore item and JPEG signature, and preserves local runtime evidence;
- successful PR #4 candidate validation in Android Foundation run `35020668795` and Platform Contract run `35020668711` for `cdc59289c607558090c298ee74ae2829268920c1`;
- successful post-merge Android Foundation run `35021425774` and Platform Contract run `35021428000` for `6dbd2c1a5c5e3fb523662f5a6e181c6dbf73644a`;
- successful PR #6 final-candidate Android Foundation run `35083327911` and Platform Contract run `35083328806` for `a378a17d3e6cf28a16521428fa3d77c1910e2e53`, including the JPEG + MediaStore emulator gate;
- successful PR #6 post-merge Android Foundation run `35083832517` and Platform Contract run `35083833021` for signed authoritative `main` commit `d5272da9877b47bfca1551784f32b1031a084a8e`;
- successful PR #7 documentation-reconciled validation and authoritative post-merge replay through `5017457a2e61716cf4eed6a0cae87922842eefc9`;
- successful PR #8 candidate Android Foundation run `35101834080` and Platform Contract run `35101835061` for `86d2d63f373ddbe7df8e6681925fafb6596972f6`;
- successful PR #8 post-merge Android Foundation run `35102550065` and Platform Contract run `35102551620` for signed authoritative `main` commit `f6d414049f6ad4792a70da00903fead403dde58c`.

The PR #8 candidate and merged revision establish source/build correctness plus regression confidence for the previously qualified preview/JPEG path. They do **not** establish video/audio runtime support because the runtime job does not exercise MediaRecorder or microphone capture.

Passing representative emulator qualification proves only the configured behavior actually exercised for that exact revision. It does not prove physical-device support, OEM camera behavior, video/audio routing, production UI quality, process-death recovery, application-specific Platform System acceptance, or production acceptance.

Future validation must cover explicit video/audio runtime capture, MP4 integrity and track inspection, microphone route changes, runtime permission revocation, physical-device qualification, media integrity, process death, camera-service restart, lock/unlock, storage exhaustion, thermal escalation, Lens isolation, Remote Viewfinder disconnect, accessibility, privacy, security, and platform integration acceptance.

## 19. Production acceptance

Camera must not be classified Stable until applicable GoreeCloud production-readiness requirements and Camera-specific gates are satisfied, including an identifiable candidate, qualified real-device matrix, no release-blocking media-loss/corruption/privacy/security defects, verified offline core capture and recovery, accepted applicable Platform System integrations, current Stable Glaze UI application-specific acceptance, synchronized repository/Drive documentation, and required human visual/usability/device acceptance.

This Concept-stage foundation and bounded still/video source implementation are not a release candidate and are not production accepted.

## 20. Known boundaries and open decisions

Open decisions include:

- final minimum Android/API support baseline beyond the provisional `minSdk 29` engineering baseline;
- whether selected future subsystems should use higher-level Android camera abstractions while Camera2 remains the advanced-control/session foundation;
- first physical-device qualification list and tier model;
- robust interrupted/process-death still/video recovery and stale-pending-row reconciliation;
- JPEG/video orientation and richer metadata policy;
- Motion Photo representation;
- codec/HDR and recording matrix beyond the current bounded H.264/AAC source path;
- audio-source/routing matrix and external microphone policy;
- Private Capture protected storage;
- provenance/authenticity format;
- Lens runtime technology;
- Remote Viewfinder transport;
- final performance budgets;
- recognized repository license.

## 21. Immediate next engineering milestone

PR #8 has satisfied exact-head source/build validation, Platform Contract validation, preview/JPEG regression qualification, merge review, and authoritative-main revalidation. It does **not** yet have video/audio runtime qualification.

The next bounded Phase 1 slice is therefore **video/audio runtime qualification**, followed by physical-device/device-profile qualification and local settings as separately governed work. The runtime slice should:

1. use an audio-capable representative Android target where microphone capture can be meaningfully exercised;
2. verify deliberate microphone authorization without automatic recording after permission grant;
3. start and stop the bounded Camera2/MediaRecorder recording path under the single session authority;
4. verify a non-zero published MP4 under the intended MediaStore destination and inspect container/video/audio tracks where the target provides representative audio input;
5. verify cleanup of handled failure paths and restoration of preview after recording;
6. distinguish representative-target success from physical-device/OEM support and retain a separate physical-device qualification gate;
7. preserve no-Internet/no-location/no-broad-storage/no-media-read authority and keep microphone authority limited to the explicit recording purpose;
8. preserve Concept lifecycle and all application-specific Platform System acceptance blockers until separately satisfied.

Broader local settings beyond the composition-grid candidate, stronger process-death recovery, licensing, current Stable Glaze UI implementation, Platform System acceptance, production signing/release, and Stable qualification remain separate obligations and must not be silently folded into a broader unreviewable change.
