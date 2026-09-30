# User-controlled portable Keyboard preference transfer

Status: **Development / explicit one-field transfer surface**

## Purpose

GoreeCloud Keyboard now exposes a native settings activity for the already-reviewed `goreecloud-keyboard-preferences/1` format.

The surface supports two explicit user actions:

- export the last emoji category the user explicitly selected; and
- import that same one-field preference from a user-selected file.

Android's Storage Access Framework supplies the source or destination document. GoreeCloud Keyboard requests no broad storage permission and does not choose a file silently.

## Privacy boundary

Version 1 remains exactly one low-sensitivity value: `EmojiCategory`.

The transfer path has no interface for and must not include:

- typed text;
- composing or surrounding editor context;
- suggestion state or learned input;
- emoji recents or frequency history;
- emoji search queries;
- clipboard contents or history;
- key-event history;
- password, PIN, payment, authentication, or other sensitive-editor content;
- telemetry identifiers;
- GoreeCloud Identity credentials or identifiers; or
- cryptographic keys or reusable secrets.

The activity reads the current category through `EmojiCategoryPreferenceReader` and applies an imported category only through `EmojiCategoryPreferenceWriter`.

## File boundary

- MIME type: `text/plain`
- suggested filename: `goreecloud-keyboard-preferences.txt`
- maximum import size: 4096 bytes
- encoding: canonical UTF-8
- inner format validation: the existing exact-record, version, checksum, and enum validation in `KeyboardPortablePreferences`

Oversized or non-canonical UTF-8 input is rejected before the preference writer can run. Malformed, tampered, expanded, unsupported, or checksum-invalid snapshots are rejected by the existing import boundary before any category write.

## Android authority boundary

The settings activity is published as the IME `settingsActivity` so Android system settings can open it. The surface uses `ACTION_CREATE_DOCUMENT` and `ACTION_OPEN_DOCUMENT`; it does not request `INTERNET`, broad external-storage access, media permissions, account access, accessibility-service authority, or any editor `InputConnection`.

The activity does not retain URI grants for background reuse and does not perform automatic transfer or synchronization.

## GLAZE UI V1.0 boundary

This is an Application settings surface. It uses the existing application theme and a 48 dp minimum interaction target for its two explicit actions. It does not establish complete GLAZE UI V1.0 rendered, accessibility, Deep Dark, representative-device, Human Visual Excellence, release, or production acceptance.

## Recovery and platform boundary

A user-controlled one-field file flow is useful portability, but it is not full Keyboard backup/restore and does not establish Everkeep acceptance. Privacy Shield, Wardveil Security, Everkeep, GoreeCloud Identity where applicable, GoreeCloud Mesh, Manager, production signing/distribution, release, and Stable qualification remain separately gated.
