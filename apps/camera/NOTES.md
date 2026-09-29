# GoreeCloud Camera — Repository Notes

> Repository document version: **0.4.0**  
> Product internal version: **0.1.0**  
> Release lifecycle: **Concept**

## Current baseline

- The stacked local-settings continuation adds a default-off, persisted composition-grid preference and non-interactive rule-of-thirds preview overlay. It is presentation-only candidate work pending exact-head validation and does not close broader Camera settings or device-qualification gates.
- The current stacked candidate includes mandatory first-use setup with persisted resume, replay, and globally disableable contextual hints. It remains Concept/Development presentation behavior only and does not create capture, permission, network, release, or Stable authority.
- Canonical repository: `GoreeCloud/goreecloud-camera`.
- Product version baseline: `0.1.0`.
- Lifecycle remains Concept; source/build progress does not establish promotion.
- Primary platform: native Android/Kotlin.
- Canonical application ID: `com.goreecloud.camera`.
- Compile/target SDK: 37 / Android 17.
- Provisional minimum SDK: 29.
- Android Gradle Plugin: 9.4.0.
- CI Gradle baseline: 9.6.0.
- Platform Contract target for this candidate: 0.4.
- Current Official Stable GLAZE UI consumer target: 1.6.0.
- Integral Platform Systems: exactly nine under v3.0 / Contract 0.4; GoreeCloud Sync remains separately governed.
- Source includes Camera2 capability discovery, lifecycle-owned preview/still/video session control, bounded JPEG still capture, and bounded Camera2/MediaRecorder MP4 recording with H.264 video plus AAC microphone audio.
- Current stacked PR #12 exact-head Android 16 / API 36 emulator evidence qualifies preview, one JPEG + MediaStore still-capture path, and the explicit microphone-permission video/audio path through a non-empty published MP4 with H.264 video, AAC audio, non-trivial duration, and preview restoration. It does not qualify physical-device/OEM camera/audio behavior or microphone signal quality.
- Authoritative PR #8 source implementation was squash-merged as signed `main` commit `f6d414049f6ad4792a70da00903fead403dde58c` after exact-head Android Foundation run `35101834080` and Platform Contract run `35101835061` passed.

- PR #12 candidate head `194ae7626d1e1a67f2a0b45719987e52e14ca343` passed Platform Contract #49 / run `35643946740` and Android Foundation #47 / run `35643945957`, including validate, preview/JPEG emulator, and video/audio emulator jobs. This stacked control-plane candidate preserves that source/evidence without claiming merge, physical-device support, or lifecycle promotion.

## Resolved engineering decisions for the foundation

- Android application identity: `com.goreecloud.camera`.
- Kotlin/native Android implementation.
- Camera2 remains the direct camera/session authority for the current foundation.
- MediaRecorder provides the bounded current MP4 encoder/recorder path.
- API 37 is the compile and target baseline.
- API 29 is a provisional development minimum, not a final support promise.
- Camera permission is required for preview/capture.
- `RECORD_AUDIO` is declared only because the bounded video-with-audio path requires it; the app requests microphone authority just in time after an explicit Record-video action, and permission grant does not auto-start recording.
- Internet, location, broad storage, all-files, and media-library read permissions remain absent.
- Core build uses AGP built-in Kotlin to avoid a redundant Kotlin Android plugin declaration.

## Open engineering decisions

- final minimum Android/API support baseline;
- whether later high-level CameraX abstractions should wrap selected flows while Camera2 remains available for advanced controls;
- initial supported hardware/qualification tiers;
- robust still/video process-death recovery and stale pending-media reconciliation;
- physical-device/OEM video/audio, microphone routing/signal quality, thermal/power, and camera-quirk qualification;
- Motion Photo representation;
- codec/HDR policy beyond the bounded H.264/AAC foundation;
- Private Capture protected-storage design;
- provenance/content-authenticity format;
- Lens runtime and future authoring model;
- Remote Viewfinder transport/pairing;
- initial performance budgets.

## Governance blockers

### License

The public repository does not yet contain a recognized open-source license. An approved license must be selected before the repository is treated as licensing-complete or approved open-source distribution. Do not infer a license from repository visibility or from unrelated GoreeCloud projects.

### Runtime conformance

All nine Integral Platform Systems remain blocked, migration-required, or otherwise unaccepted. Source/runtime emulator evidence does not establish application-specific Platform System acceptance; Policy and Observability are now explicitly evaluated under Contract 0.4.

### Device qualification

No supported real-device Camera preview/capture qualification has been recorded. Representative virtual-camera evidence, including PR #12 video/audio qualification, is not a supported-device/OEM or microphone-quality claim.

### User manual synchronization

The central Camera user-manual record exists at `GoreeCloud/User Manuals/User Manual — GoreeCloud Camera.md` and must remain synchronized with verified repository behavior. It must preserve the distinction between source/build implementation and runtime/device qualification.

## Next engineering milestone

After Contract 0.4 reconciliation, the next source-controlled presentation milestone is repository-local GLAZE UI V1.6 mapping without relabeling the engineering shell as accepted. Physical-device/OEM capture and microphone qualification, local settings, process-death/stale-pending recovery, licensing, nine-system runtime acceptance, signing/release provenance, Production Acceptance, and Stable qualification remain separate open gates.
