# GoreeCloud Keyboard Security

GoreeCloud Keyboard is a Development-stage Android input method maintained in `GoreeCloud/android-app-defaults` under `apps/keyboard/`. Typed content, editor context, clipboard data, learned input, and usage-derived history are highly sensitive. Security and privacy behavior is fail-closed and evidence-gated; source presence or green CI does not establish production or Stable acceptance.

## Current trust boundary

- The Android application does not request general network access.
- The IME service is exposed only for the Android input-method role and is protected by `android.permission.BIND_INPUT_METHOD`.
- Android automatic application backup remains disabled for Keyboard-owned sensitive state.
- Quill suggestions, correction, prediction, grammar assistance, emoji search, and swipe decoding are device-local.
- Missing or unknown editor metadata fails closed toward the more restrictive editor policy.
- Sensitive/password editors suppress suggestion capture/use, optional learning, swipe input, and other behavior that would expand text observation.
- Editor-session transitions reset transient input state so one editor session does not silently lend policy or content context to another.
- No telemetry, advertising, sponsorship, remote analytics, or remote learning authority is implied.

## Typed text and editor context

Keyboard must collect only the minimum transient text context required for the active approved input behavior. Availability of `InputConnection` is not general authority to inspect, retain, export, synchronize, or log editor contents.

Unless a separately reviewed capability explicitly authorizes the exact scope, do not persist, transmit, synchronize, back up, export, or log:

- typed or composing text;
- surrounding editor content;
- passwords, one-time codes, authentication tokens, recovery codes, or private keys;
- raw gesture traces;
- key-event history;
- sensitive-editor contents;
- raw clipboard payloads outside the explicit local Clipboard-history boundary below;
- telemetry identifiers or behavioral profiles.

## Optional local learning

Optional learning is user-controlled and off by default. When enabled, Keyboard may retain only the bounded local word/bigram counters defined by the current implementation. Sensitive editors, host no-suggestions editors, and editors requesting Android no-personalized-learning are excluded from collection and from use of learned personalization. Learned data remains device-local and user-clearable.

## Clipboard boundary

The Keyboard-side Clipboard feature is not the future privileged GoreeCloud Secure Paste Broker.

Current candidate behavior permits explicitly enabled, device-local encrypted text history with bounded retention. Saved clips are encrypted with Android Keystore-backed AES-GCM, history is bounded, Android-marked sensitive clips are never persisted, and derived smart-content fragments are not separately persisted. Clipboard content is excluded from backup, synchronization, Quill learning, prediction, correction, and personalization.

Per-app **Allow / Ask / Paste only / Block** settings control Keyboard-side behavior only. A normal Android IME cannot revoke another application's operating-system clipboard authority.

## Portability and recovery

Portable Keyboard preferences must remain privacy-minimized. Typed/editor content, learned text, raw clipboard history, credentials, secrets, and other usage-derived sensitive content must not silently enter export/import formats.

Imports must validate the complete input before mutation and fail closed on malformed, oversized, unsupported, tampered, expanded, or noncanonical data. Export/import behavior must not be represented as complete backup, Everkeep recovery, or clean-target recovery unless those separate acceptance gates are satisfied.

## Accessibility and interaction security

Accessibility nodes, long-press alternates, toolbar controls, Clipboard controls, setup surfaces, and custom-drawn input controls must reuse existing bounded action paths rather than creating independent text-read or commit authorities. Accessibility behavior may consume rendered geometry and labels needed for interaction, but it must not gain clipboard, persistence, network, surrounding-text, learning, or telemetry authority merely to expose semantics.

## Platform-system and dependency security

Keyboard must substantively address the applicable Integral Platform Systems: Manager, Privacy Shield, Wardveil Security, Everkeep, GLAZE UI, Mesh, Identity, Policy, and Observability. A manifest entry, label, icon, status card, or local defensive behavior is not by itself accepted runtime integration.

Keep signing keys, keystores, passwords, reusable tokens, private keys, recovery secrets, and production credentials outside Git, issues, pull requests, logs, screenshots, and ordinary documentation.

Any future network permission, remote model, remote dictionary, voice-input service, media/GIF source, or remote assistance path requires explicit product authority, endpoint and retention documentation, authentication/authorization, offline/failure behavior, Privacy Shield review, Wardveil review, and acceptance before production use.

## Vulnerability handling

Do not place raw typed text, clipboard payloads, credentials, private device details, or reusable secrets in public issues. Use GitHub private vulnerability reporting when available; otherwise disclose only the minimum non-sensitive information needed to establish the problem and arrange an appropriate private handoff.

## Release boundary

Keyboard remains Development. Production or Stable qualification still requires current GLAZE UI application acceptance, representative physical-device typing/latency testing, TalkBack/Switch Access/Voice Access/Touch Assistance and broader accessibility acceptance, RTL/localization and supported form-factor validation, accepted platform-system integrations where applicable, protected signing/provenance, recovery acceptance, release verification, and explicit production approval.
