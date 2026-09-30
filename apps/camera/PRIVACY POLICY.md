# GoreeCloud Camera — Privacy Policy

> Repository document version: **0.4.0**  
> Product internal version: **0.1.0**  
> Release lifecycle: **Concept**  
> Runtime privacy status: **Local camera + explicit video microphone authority; Privacy Shield acceptance pending**

## Current implemented boundary

The manifest requests `android.permission.CAMERA` and `android.permission.RECORD_AUDIO`.

Camera permission is used for local preview and capture. Microphone permission exists only for the bounded video-with-audio workflow and is requested just in time after the user deliberately chooses **Record video**. Granting microphone permission does not automatically begin recording; the user must deliberately initiate recording again.

The app does **not** request Internet, fine/coarse location, broad storage, all-files, or media-library read permissions for the current milestone.

The Camera2 preview path displays live frames locally. Current photo/video output is written only to newly created MediaStore items owned by GoreeCloud Camera. The app does not read the user's existing media library as part of this milestone.

## Still-photo publication

A photo destination is first reserved as an `IS_PENDING` MediaStore image under `DCIM/GoreeCloud Camera`. Other apps should not see the item as a completed photo while it remains pending. After a successful JPEG write, Camera clears the pending state. Handled capture or write failures delete the reserved row rather than intentionally publishing an incomplete file.

## Video/audio publication

A video destination is reserved as a pending MediaStore video under `DCIM/GoreeCloud Camera`. The source/build implementation records MP4 using H.264 video and AAC microphone audio, then publishes only after recorder finalization and a non-zero MediaStore size check. Handled preparation, recording, stop, or finalization failures discard the pending row where the process remains alive.

PR #12 exact-head Android 16 emulator qualification exercises the explicit microphone-permission video/audio path and verifies a non-empty published MP4 with H.264 video, AAC audio, non-trivial duration, and preview restoration. This does not establish physical-device/OEM microphone routing, signal quality, production privacy acceptance, or supported-device claims.

## Network, location, accounts, and downstream services

This milestone adds no networking, location, account, synchronization, remote-control, Lens, or downstream-service authority. Core capture remains designed to work offline.

Gallery, Photos, Everkeep, Sync, or other services must not receive media merely because they are installed. Future handoff, backup, sharing, or synchronization requires its own effective authorization and documented contract.

## Metadata

The current MediaStore paths set display name, MIME type, relative path, and publication state. Location capture is not implemented. Future Original, Privacy Safe, and Custom metadata policies remain planned.

## Camera and microphone data

Preview frames remain ephemeral by default. A newly captured JPEG is persisted only after an explicit shutter action. Microphone input is intended to be active only during an explicitly started video recording after microphone authorization. No production diagnostic path is authorized to retain raw preview frames, captured audio, or media payloads.

## Privacy Shield

No Camera-specific Privacy Shield runtime acceptance exists yet. Applicable future policy integration must be operation-bound, purpose-limited, and fail closed for dependent features.

## Changes

Revise this policy whenever implementation materially changes camera-frame handling, microphone/audio handling, metadata, diagnostics, networking, accounts, extensions, remote access, backup, synchronization, sharing, or other privacy-relevant behavior.
