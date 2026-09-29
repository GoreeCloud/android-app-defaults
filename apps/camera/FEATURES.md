# GoreeCloud Camera — Current Features

> Repository document version: **0.6.0**  
> Product internal version: **0.1.0**  
> Release lifecycle: **Concept**

## Verified runtime foundation

Representative Android 16 / API 36 virtual-camera CI evidence verifies the following bounded behavior on the configured emulator:

- application installation and camera permission grant;
- Camera2 device discovery;
- selected emulated back-camera opening;
- preview session configuration;
- repeating preview reaching application state `PREVIEWING`;
- one explicit JPEG still-capture request;
- publication of a matching non-zero MediaStore JPEG under `DCIM/GoreeCloud Camera`;
- readback and JPEG-signature verification of the published artifact.

The preview/JPEG regression also passed on final PR #8 candidate `86d2d63f373ddbe7df8e6681925fafb6596972f6` in Android Foundation run `35101834080`. This evidence verifies the bounded preview/still path only; it does not qualify a physical device or exercise video/audio runtime behavior.

## Implemented source and build functionality

The current stacked candidate additionally implements a mandatory, resumable three-step first-use guide before preview, replayable **Help & guidance**, and a global contextual-hints preference. Guidance is presentation-only and does not expand Camera2, microphone, storage, network, cloud, or release authority.

The stacked local-settings continuation adds two app-private controls: a persisted **Composition grid** toggle with a non-interactive rule-of-thirds overlay, and a default-on **Volume keys capture photo** preference. Volume Up/Down reuse the existing still-capture action only on the first key-down while the session is already PREVIEWING and no Camera modal is open; repeated key events, non-preview states, and modal surfaces fail closed. These controls add no Camera2 session, MediaStore, permission, microphone, network, account, or cloud authority and require fresh exact-head validation before they are treated as verified Development evidence.

Authoritative `main` commit `f6d414049f6ad4792a70da00903fead403dde58c` includes:

- JPEG output capability discovery and JPEG `ImageReader` capture output;
- one-shot `TEMPLATE_STILL_CAPTURE` requests;
- engineering **Capture photo** control with bottom-system-bar inset handling;
- deterministic UTC JPEG naming;
- MediaStore JPEG reservation/publication under `DCIM/GoreeCloud Camera` with handled-failure pending-row cleanup;
- bounded video-size discovery for MediaRecorder-compatible output;
- Camera2 `TEMPLATE_RECORD` recording sessions using preview plus recorder surfaces;
- MP4 output using H.264 video plus AAC microphone audio;
- deterministic UTC `GCAM_*.mp4` naming;
- pending MediaStore video reservation, non-zero-size validation before publication, publish, and handled-failure discard;
- microphone-hardware capability gating;
- explicit just-in-time `RECORD_AUDIO` authorization after a deliberate Record-video action;
- no automatic recording after microphone permission grant;
- visible recording, stopping, microphone-active, saved, and failure states in the engineering shell;
- lifecycle cleanup and preview restoration around recording;
- no Internet, location, broad storage, all-files, or media-library read permission expansion;
- static source-contract checks for photo/video capture, permissions, MediaStore behavior, and lifecycle boundaries;
- unit coverage for deterministic photo/video filename generation;
- exact-revision lint, unit tests, APK assembly, provenance/digest verification, artifact upload, and Platform Contract validation.

## Evidence boundary

The video/audio implementation is **source/build verified**, not runtime qualified. The current emulator workflow is configured to re-qualify preview/JPEG behavior and does not prove that MP4 recording, AAC capture, microphone routing, encoder behavior, duration/finalization, or audio/video synchronization works on a representative audio-capable device.

## Not yet implemented or accepted

- video/audio runtime qualification and microphone-routing evidence;
- physical-device qualification and device-profile/quirk records;
- broader local settings beyond the persisted composition-grid and volume-key shutter candidate;
- robust interrupted/process-death photo and recording recovery;
- zoom/focus/exposure controls and lens switching UI;
- production Glaze UI components and acceptance;
- Private Capture;
- richer metadata/provenance controls;
- Gallery/Photos handoff;
- accepted application-specific Platform System integrations;
- production package/release and Stable qualification;
- recognized open-source repository license.

## Status rule

Planned capabilities remain in `FEATURE-ROADMAP.md`. Source implementation, build verification, representative-emulator runtime qualification, physical-device qualification, and production acceptance are separate evidence levels and must not be conflated.
