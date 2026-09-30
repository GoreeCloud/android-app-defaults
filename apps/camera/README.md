# GoreeCloud Camera

> Repository document version: **0.6.0**  
> Product internal version: **0.1.0**  
> Release lifecycle: **Concept**

GoreeCloud Camera is the first-party photography, video, scanning, and creative-capture application planned for GoreeCloud mobile devices and supported Android phones.

## Current state

The current stacked candidate is a native Android Camera2 implementation. Representative Android 16 / API 36 emulator qualification has verified preview, bounded JPEG still capture + MediaStore publication, and the explicit microphone-permission video/audio path through a published non-empty MP4 with H.264 video, AAC audio, non-trivial duration, and preview restoration. This is representative-emulator evidence only and does not qualify a physical device, OEM camera/audio path, microphone signal quality, production UI, or release readiness.

Authoritative `main` now also includes the first bounded video/audio **source and build** foundation from PR #8, merged as signed commit `f6d414049f6ad4792a70da00903fead403dde58c`. It adds capability-gated Camera2/MediaRecorder MP4 recording using H.264 video and AAC microphone audio, deterministic UTC MP4 naming, pending MediaStore publication/cleanup, explicit recording state, microphone hardware gating, and just-in-time microphone permission requested only after a deliberate Record-video action. Exact PR-head Android Foundation run `35101834080` and Platform Contract run `35101835061` passed.

PR #12 re-qualifies the corrected current candidate after the MediaStore finalization fix. Exact head `194ae7626d1e1a67f2a0b45719987e52e14ca343` passed Platform Contract #49 / run `35643946740` and Android Foundation #47 / run `35643945957`; that Android run passed source/build validation, preview/JPEG qualification, and the separate video/audio emulator lane. The result remains Development/Concept emulator evidence rather than physical-device/OEM acceptance.

The release lifecycle remains **Concept**. Emulator success, source presence, and a buildable APK do not establish supported devices, production UI acceptance, or release readiness.

## Implemented foundation

- Mandatory three-step first-use guidance now precedes preview in the current stacked Development candidate, persists an interrupted step, can be replayed from **Help & guidance**, and exposes a global contextual-hints toggle.
- First-use language keeps Camera/microphone permission scope, local MediaStore capture, hardware-dependent capability, and Concept/Development limits explicit; it adds no network or cloud authority.
- The stacked local-settings candidate adds an app-private **Composition grid** preference, default off, with a non-interactive rule-of-thirds preview overlay and persisted restart behavior. It does not change camera, microphone, storage, or network authority; fresh exact-head validation is still required.
- Canonical Android application ID: `com.goreecloud.camera`.
- Product version: `0.1.0`.
- Compile/target baseline: Android 17 / API 37; provisional minimum SDK: API 29.
- JDK 17, Android Gradle Plugin 9.4.0, and Gradle 9.6.0 in CI.
- Runtime manifest requests `android.permission.CAMERA` and `android.permission.RECORD_AUDIO`.
- Camera permission is needed for preview/capture; microphone permission is requested only when the user deliberately chooses video recording with audio.
- Camera2 device enumeration, capability profiles, deterministic default-camera selection, and lifecycle-owned session control.
- Representative Android 16 / API 36 emulated-back-camera preview qualification.
- Verified representative-emulator JPEG still capture using `TEMPLATE_STILL_CAPTURE` and `ImageReader`.
- Verified representative-emulator MediaStore photo commit path to `DCIM/GoreeCloud Camera` using `IS_PENDING`, publish-after-write, and pending-row deletion on handled failure.
- Bounded source/build implementation for Camera2/MediaRecorder MP4 recording using H.264 video plus AAC microphone audio.
- Deterministic UTC `GCAM_*.mp4` naming and pending MediaStore video publication/cleanup.
- No Internet, location, broad storage, all-files, or media-library read permission.
- Exact-revision source checks, lint, unit tests, APK build/provenance, Platform Contract validation, and preview/JPEG emulator regression qualification.

## Still open

- physical-device preview, still, video/audio, microphone-route/signal-quality, OEM, thermal, power, and camera-quirk qualification;
- broader local settings beyond the current composition-grid candidate;
- zoom/focus/exposure and lens-switching controls;
- robust process-death/interrupted-capture and interrupted-recording recovery;
- production Glaze UI implementation and acceptance;
- Privacy Shield, Wardveil Security, Everkeep, Manager, Mesh, and Identity application acceptance where applicable;
- device-specific qualification profiles and camera-quirk evidence;
- Private Capture, richer metadata/provenance controls, and Gallery/Photos handoff;
- production signing/release and Stable qualification;
- recognized open-source license selection.

## Development

With matching local tooling installed:

```bash
gradle --no-daemon lintDebug testDebugUnitTest assembleDebug
```

The CI debug APK remains an engineering artifact only.

## Privacy boundary

Core capture remains local and offline-first. Android 10+ MediaStore lets Camera publish media that it creates without requesting broad storage or media-library read permissions. Camera currently uses camera authority for preview/capture and microphone authority only for the explicit video-with-audio workflow. It does not request Internet or location authority, and it does not read the user's existing media library as part of the current milestones.

## GoreeCloud platform direction

- Platform Contract target for this candidate: **0.4**.
- Current required GLAZE UI consumer target: **1.6.0**.
- Nine Integral Platform Systems are evaluated explicitly: GoreeCloud Manager, Privacy Shield, Wardveil Security, Everkeep, Glaze UI, GoreeCloud Mesh, GoreeCloud Identity, GoreeCloud Policy, and GoreeCloud Observability.
- GoreeCloud Sync remains separately governed and is not a tenth Integral Platform System.

## Repository documentation

- [SPECIFICATIONS.md](SPECIFICATIONS.md)
- [FEATURES.md](FEATURES.md)
- [FEATURE-ROADMAP.md](FEATURE-ROADMAP.md)
- [BENEFITS.md](BENEFITS.md)
- [COMPETITIVE-OBJECTIVES.md](COMPETITIVE-OBJECTIVES.md)
- [BRANDING.md](BRANDING.md)
- [USER-MANUAL.md](USER-MANUAL.md)
- [PRIVACY POLICY.md](PRIVACY%20POLICY.md)
- [SECURITY.md](SECURITY.md)
- [NOTES.md](NOTES.md)
- [goreecloud.platform.yaml](goreecloud.platform.yaml)

## License

A recognized open-source license has not yet been selected and committed. Repository visibility does not itself grant reuse rights.

## Status integrity

The current work does not prove Experimental lifecycle eligibility, physical-device/OEM support, GLAZE UI V1.6 application conformance, any Integral Platform System runtime acceptance, production readiness, or Stable status.
