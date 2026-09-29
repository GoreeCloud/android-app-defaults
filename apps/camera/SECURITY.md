# GoreeCloud Camera — Security

> Repository document version: **0.4.0**  
> Product internal version: **0.1.0**  
> Release lifecycle: **Concept**  
> Runtime security status: **Bounded session/media-finalization controls implemented; Android 16 emulator video/audio qualified; physical-device/OEM and Wardveil acceptance pending**

## Current status

`CameraSessionController` centralizes Camera2 device/session ownership and owns the current bounded still and video recording lifecycles. Activity pause, preview-surface destruction, stop, and shutdown close active camera/session/ImageReader/MediaRecorder resources and discard handled in-process pending output where applicable.

Photo and video paths use pending MediaStore rows. Successful photo publication follows JPEG validation/write completion. Successful video publication follows MediaRecorder stop/finalization plus a non-zero MediaStore size check. Handled failures discard the pending row where the process remains alive.

These controls are source/runtime engineering boundaries, not Wardveil Security acceptance or production recovery qualification.

## Current permission boundary

The manifest requests camera and microphone permissions. Camera authority supports preview/capture. Microphone authority exists only for video-with-audio and is requested just in time after an explicit Record-video action; granting it does not automatically start recording.

The app does not request Internet, location, broad storage, all-files, or media-library read permissions. No remote-control, Lens, account, synchronization, or protected-storage path exists.

## Media integrity boundary

The photo committer validates the JPEG signature before publication. The video committer keeps the MediaStore row pending until the recording is finalized and the stored size is non-zero. Invalid/failed handled output is treated as capture failure and the pending row is discarded where possible.

Process-death recovery, storage exhaustion handling, transactional journaling, interrupted-recording recovery, and protected/private destinations remain future reliability/security work.

## Recording security boundary

The current source/build implementation gates video recording on a compatible bounded video output, microphone hardware, camera readiness, and microphone permission. The engineering UI exposes active microphone state during recording. PR #12 exact-head Android 16 emulator qualification verifies the bounded video/audio path at the emulator level. No claim is made for physical-device/OEM microphone routing or signal quality, sustained recording, thermal/power behavior, or production security acceptance.

## Planned security boundaries

Protect active camera-session ownership, temporary frames, RAW data, cached/in-progress video, microphone sessions, Private Capture media/temp files, camera/microphone permissions, location metadata, Remote Viewfinder sessions, Lens packages/permissions, and applicable downstream credentials.

## Diagnostics

Ordinary diagnostics must not contain captured media, raw preview frames, or recorded microphone audio by default. CI qualification artifacts may contain synthetic emulator still-capture evidence used solely as non-user test evidence; this does not authorize production diagnostics to collect user media or audio.

## Reporting security issues

Do not publish active secrets, private user data, or detailed exploitable vulnerability material in a public issue. Use an approved private GoreeCloud security-reporting channel.

## Release impact

Known defects involving media integrity, protected storage, permission bypass, unauthorized camera/microphone activation, unauthorized remote access, extension sandbox escape, or privacy/security boundary failure are release-blocking for the affected capability.
