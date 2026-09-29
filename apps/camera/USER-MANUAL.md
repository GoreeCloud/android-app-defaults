# GoreeCloud Camera — User Manual

> Repository document version: **0.5.0**  
> Product internal version: **0.1.0**  
> Release lifecycle: **Concept**  
> Manual status: **Engineering capture foundation**

## Current availability

The repository contains a buildable native Android Camera2 foundation. Representative Android 16 / API 36 emulator evidence verifies preview and one bounded JPEG + MediaStore still-capture path. No physical device is qualified and this is not a supported release.

Authoritative source also includes a bounded engineering video/audio workflow. Source checks, unit tests, lint, and APK assembly pass, but video/audio recording and microphone routing have not yet been runtime-qualified on an audio-capable representative target.

## Current engineering workflow

1. Build the exact repository revision with JDK 17, Gradle 9.6.0, Android SDK 37, and AGP 9.4.0.
2. Install the generated debug APK on a development device or emulator.
3. Launch **GoreeCloud Camera** and grant camera permission.
4. Wait for the engineering status to show `Session: previewing`.
5. Use **Capture photo** to request a JPEG still. On the verified bounded path, the status shows `Photo saved: <filename>` and the image is published under `DCIM/GoreeCloud Camera` through MediaStore.
6. Where the current device reports a compatible bounded video output and microphone hardware, **Record video** becomes available.
7. The first deliberate Record-video action requests microphone permission if it has not been granted. Granting the permission does not auto-start recording.
8. Deliberately choose **Record video** again after permission is granted to request H.264 MP4 video with AAC microphone audio. During an active recording the engineering shell reports that the microphone is active and exposes **Stop and save video**.
9. Leaving the activity closes the active camera/session outputs and cleans up handled in-process pending capture state.

Steps 6–8 describe the implemented engineering control path. They do not constitute a runtime-support claim until separate video/audio qualification evidence exists.

## Permission behavior

- `android.permission.CAMERA` — required for preview and capture.
- `android.permission.RECORD_AUDIO` — requested only after the user deliberately chooses video recording with audio.

The current milestone does not request Internet, location, broad storage, all-files, or media-library read permissions.

## Local settings candidate

After completing first-use setup, open **Settings** from the Camera top controls. **Composition grid** is off by default. Turning it on shows a non-interactive rule-of-thirds overlay over the live preview; turning it off removes the overlay. The preference is stored only in Camera app-private settings and survives ordinary app restart.

This setting changes preview presentation only. It does not grant Camera or microphone permission, change captured media, upload anything, or enable additional camera hardware.

## Current limitations

The interface remains an engineering shell. Video/audio representative-device qualification, microphone-routing evidence, physical-device support, broader local settings beyond the composition grid, zoom/focus/exposure UI, production Glaze UI, Private Capture, rich metadata policy, downstream Gallery/Photos handoff, interrupted/process-death recovery, and production signing remain unqualified or unimplemented.

A visible `Photo saved` or `Video saved` result is engineering-state feedback; supported-device claims require the separate qualification process and artifact verification appropriate to the capability.

## Troubleshooting

- If camera permission is denied, use the on-screen camera permission button and Android permission controls.
- If microphone permission is denied, preview and photo capture can remain available; video-with-audio recording remains off.
- If no compatible bounded video output or microphone capability is reported, the app keeps video recording unavailable rather than inventing support.
- If photo/video finalization fails while the process remains alive, the implementation attempts to discard the pending MediaStore row and reports failure.
- A source/build/CI success does not qualify a physical phone or prove video/audio runtime behavior.

The required GoreeCloud-wide representation is `GoreeCloud/User Manuals/User Manual — GoreeCloud Camera.md`; this repository `USER-MANUAL.md` remains the canonical editable source.


## Development first-use guidance

On first launch of the current Development candidate, Camera shows a three-step setup guide before preview. If setup is interrupted, the current step resumes on the next launch. After completion, **Help & guidance** can replay the guide and can turn ordinary contextual hints on or off globally. These guidance controls do not grant Camera or microphone permission and do not upload media.
