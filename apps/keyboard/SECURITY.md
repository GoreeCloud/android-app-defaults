# GoreeCloud Keyboard Security

GoreeCloud Keyboard is a Development-stage Android input method. Typed content, editor context, clipboard data, learned input, and usage-derived history are highly sensitive. Security and privacy behavior in this repository is therefore fail-closed and evidence-gated; source presence or successful CI does not establish production or Stable acceptance.

## Current trust boundary

- The authored Android application requests no general Android permissions and does not request network access.
- The IME service is exported only for the Android input-method role and requires `android.permission.BIND_INPUT_METHOD` from the binding system.
- Android automatic application backup is disabled.
- The current first-party suggestion path is local-only.
- Missing or unknown editor metadata fails closed as sensitive and suggestions-suppressed.
- Password/sensitive editors suppress suggestion collection, display, and acceptance and do not use surrounding-text inspection for the ordinary suggestion path.
- Host editors that request no suggestions or no personalized learning are honored without weakening the stricter sensitive-editor policy.
- Editor-start and editor-finish callbacks reset transient composing, suggestion, shift/layer, and presentation state so one editor session cannot silently lend policy or content context to another.
- Current emoji recents and remembered emoji-category state are device-local. They are not network-backed or account-backed.

## Text and editor-data handling

Keyboard must collect only the minimum text context required for the active, approved input behavior. New code must not treat availability of `InputConnection` as general authority to inspect or retain editor contents.

The following data must not be persisted, transmitted, synchronized, logged, included in diagnostics, or placed in portability artifacts unless a separately reviewed capability explicitly authorizes the exact scope:

- typed text or composing text;
- surrounding editor contents;
- passwords, one-time codes, authentication tokens, recovery codes, or private keys;
- suggestion or learned-input history;
- key-event history;
- clipboard contents or clipboard history;
- sensitive-editor contents or classifications tied to content;
- application/editor identifiers derived from typing sessions;
- telemetry identifiers or behavioral profiles.

Ordinary operational logs and CI evidence must never include raw editor contents or clipboard payloads.

## Portability and recovery

The current Development portability contract is intentionally narrow: `goreecloud-keyboard-preferences/1` carries only the last emoji category explicitly selected by the user.

The portability boundary must remain privacy-minimized:

1. Validate the entire input before any write.
2. Reject malformed, oversized, unsupported, tampered, expanded, or noncanonical input without partial mutation.
3. Preserve preview-before-Apply import behavior.
4. Preserve review/freeze-before-destination export behavior.
5. Keep typed/editor content, learned input, emoji recents/frequency history, emoji search queries, clipboard data, credentials, secrets, and other usage-derived history outside the format.
6. Treat Storage Access Framework selection as explicit user authority for one operation, not background storage or synchronization authority.
7. Do not represent one-field preference transfer as complete backup, restore, Everkeep integration, or clean-target recovery.

Any schema expansion requires separate privacy, retention, consent, recovery, compatibility, and threat review.

## Accessibility and interaction security

Virtual accessibility nodes, long-press alternates, emoji controls, suggestion controls, and other custom-drawn interaction surfaces must reuse existing semantic input paths rather than creating second editor-data or commit authorities.

Accessibility code may consume already-rendered control geometry and labels needed for interaction, but must not gain clipboard, persistence, network, surrounding-text, learned-input, or telemetry authority merely to expose accessible semantics.

Long-press alternate geometry and pointer hit testing must fail closed when a valid rendered target cannot be established. Pointer release must not commit an invisible or unresolved alternate.

## Secure Paste boundary

Secure Paste is planned architecture, not current enforcement. A normal Android IME cannot by itself revoke another application's operating-system clipboard authority. Any future Secure Paste implementation requires the separately approved Privacy Shield policy and privileged platform enforcement boundary described in the canonical project specification.

Clipboard payloads must not be uploaded, synchronized, backed up, or associated with Identity by default. Any future clipboard persistence or synchronization is a separate capability requiring explicit user control and acceptance.

## Platform-system integration

Keyboard is a GoreeCloud application and must substantively address Manager, Privacy Shield, Wardveil Security, Everkeep, Glaze UI, GoreeCloud Mesh, and GoreeCloud Identity where applicable. Current `goreecloud.platform.yaml` remains the machine-readable repository declaration.

A platform-system integration is not accepted merely because a manifest entry, icon, label, status card, or local defensive behavior exists. Acceptance requires the applicable runtime, authorization, privacy, security, resilience, failure-mode, accessibility, and evidence gates.

## Dependency, source, and release security

- Keep signing keys, keystores, passwords, reusable tokens, private keys, recovery secrets, and production credentials outside Git, issues, pull requests, CI logs, screenshots, and ordinary documentation.
- Do not add ad, sponsorship, tracking, remote analytics, remote-learning, or remote-content dependencies as convenience features.
- Any future network permission or remote assistance path requires explicit product authority, data minimization, endpoint documentation, authentication/authorization, failure handling, offline behavior, logging boundaries, Privacy Shield review, and Wardveil review before production acceptance.
- Preserve exact-head CI and emulator evidence for source-sensitive changes.
- Release signing and distribution remain separately governed and must not be inferred from debug APK success.

## Vulnerability handling

Do not post raw sensitive text-entry data, clipboard payloads, credentials, private device details, or reusable secrets in public issues. Use the repository's private GitHub security-reporting channel when configured. If no private channel is available, disclose only the minimum non-sensitive information needed to establish the problem and request an appropriate private handoff before sharing sensitive reproduction material.

## Release boundary

Keyboard remains Development and nonconformant. Production or Stable qualification still requires current Glaze UI application acceptance, representative physical-device typing and latency validation, TalkBack/Switch Access/Voice Access/Touch Assistance and broader accessibility acceptance, RTL/localization and supported form-factor validation, accepted Privacy Shield/Wardveil/Everkeep/Identity/Mesh/Manager integration where applicable, protected signing/provenance, recovery acceptance, release verification, and explicit production approval.
