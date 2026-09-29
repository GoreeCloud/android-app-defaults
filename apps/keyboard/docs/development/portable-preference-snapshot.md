# Portable Preference Snapshot — Development Privacy Boundary

## Status

Development source foundation only. This checkpoint does **not** establish complete Keyboard backup, restore, export, import, Everkeep integration, Privacy Shield acceptance, or production recovery.

## Purpose

`KeyboardPortablePreferences` defines a deliberately minimal versioned portability envelope for explicit low-sensitivity Keyboard preference state without expanding the Keyboard's authority to observe, retain, synchronize, or export typed content or usage-derived history.

Current format identifier:

`goreecloud-keyboard-preferences`

Current format version:

`1`

## Included state

Version 1 contains exactly one portable value:

- the last explicitly selected emoji category.

This value already exists as a bounded private on-device preference through `LocalEmojiCategoryStore`. The snapshot does not read that store itself; callers must explicitly supply an already-materialized `EmojiCategory` value.

## Explicitly excluded sensitive or usage-derived state

The current format must not contain:

- typed text;
- composing text or surrounding editor context;
- suggestion candidates or suggestion-learning state;
- dictionary learning derived from user input;
- emoji recents or frequency history;
- emoji search queries;
- clipboard contents or clipboard history;
- key-event history;
- passwords, PINs, payment data, authentication fields, or other sensitive-editor content;
- application/editor identifiers derived from typing sessions;
- telemetry identifiers;
- GoreeCloud Identity credentials or identifiers; or
- cryptographic keys or reusable secrets.

Emoji recents are specifically excluded even though the current application stores a bounded list locally. Recents are usage-derived history and therefore have a different privacy character from an explicit category preference. A future decision to synchronize or export recents would require a separate privacy, consent, retention, and platform-authority review rather than an implicit schema extension.

## Canonical representation and validation

The encoded form is a bounded UTF-8 line format with canonical LF endings and exactly four records:

1. format identifier;
2. version;
3. `emoji_category` enum name; and
4. SHA-256 checksum.

The decoder rejects oversized snapshots, CRLF/non-canonical line endings, extra or missing records, unsupported format/version values, malformed checksums, integrity mismatches, and unknown emoji categories.

The checksum detects corruption or unintended modification. It is not encryption, authentication, a signature, or authorization.

## Authority boundary

Encoding and decoding are pure data transformations. They do not:

- request Android input/editor context;
- open an `InputConnection`;
- inspect current or previous typed text;
- read emoji recents;
- query clipboard state;
- write SharedPreferences;
- contact GoreeCloud Identity, Everkeep, Mesh, Privacy Shield, or another service;
- request network access; or
- alter IME enablement/default status.

A future import coordinator must separately define explicit user control, destination ownership, conflict behavior, transactional persistence, and accepted Privacy Shield/Everkeep authority.

## Backup and Everkeep boundary

This one-field snapshot is not the complete Keyboard recovery contract. Before Keyboard backup/restore can be considered implemented or verified, GoreeCloud still requires an approved minimal product-owned durable-state inventory, explicit exclusion rules for sensitive/usage-derived data, transactional clean-target restore, version migration tests, corruption/failure testing, privacy/security acceptance, and successful Everkeep backup/restore evidence.

## Acceptance boundary

Passing unit tests for this codec proves only the source-level format and privacy-minimized schema at the tested revision. It does not qualify Keyboard for Stable, user-facing import/export, synchronization, Everkeep recovery, Privacy Shield acceptance, or production deployment.
