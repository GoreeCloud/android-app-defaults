# GoreeCloud Gallery 1.0.0 Stable Candidate

## Status

The maintained source line now prepares the final semantic package version `1.0.0` with Android `versionCode` `10009` through gc.9.

This document does **not** declare GoreeCloud Gallery Stable. Version identity and release classification are deliberately separate. Until all blocking evidence is complete, every `1.0.0` APK remains either an Acceptance Candidate or a Signed Release Candidate.

## Why the final version is used before Stable approval

GoreeCloud validates the exact binary intended for release. The candidate therefore carries the final semantic version before promotion so Stable publication does not require rebuilding a different APK merely to change a version suffix.

Promotion to Stable must reuse the exact signed candidate binary whose checksum, signer fingerprint, device acceptance, accessibility, Glaze UI review, upgrade/recovery behavior, and release evidence were accepted.

## Source and product invariants

The `1.0.0` candidate preserves the validated GoreeCloud behavior established through gc.8:

- application ID `com.goreecloud.gallery`;
- no `android.permission.INTERNET`;
- no analytics, advertising, tracking, cloud account, or remote API dependency added by GoreeCloud;
- Android OS user/profile isolation and the application sandbox remain the multi-user/private-data boundary;
- Glaze UI remains mandatory for GoreeCloud-controlled surfaces;
- square-thumbnail presentation remains unavailable;
- file and folder thumbnails remain rounded;
- toolbar overflow presentation remains readable in accepted light/dark Glaze UI states;
- GoreeCloud-owned behavioral tests remain required and fail closed in CI.

## Required Stable evidence

Stable publication remains blocked until all of the following exist and are reviewed:

1. Long-lived Android release signing identity with independently protected and verified recovery copies.
2. Protected `stable-release` GitHub environment with required signing secrets and reviewer controls.
3. A successful signed `1.0.0` candidate workflow whose signer fingerprint matches the approved public certificate fingerprint.
4. Repository-level `main` protection/ruleset that prevents routine bypass of required acceptance validation.
5. Representative-device acceptance covering permissions, browsing, playback, favorites, copy/move/rename, destructive operations on disposable media, hidden/excluded content, editing, sharing, lifecycle behavior, and Android user/profile isolation.
6. Glaze UI light/dark, dialog, popup, thumbnail, destructive-action, and overall presentation acceptance.
7. Accessibility acceptance including TalkBack, large fonts, display scaling, focus, contrast, touch targets, and motion behavior.
8. Same-signer in-place upgrade validation and documented bad-release recovery validation without damage to local media.
9. Complete release evidence recording commit, workflow run, APK SHA-256, signer fingerprint, device/Android version, test outcomes, license/provenance review, and deliberate Stable approval.

Use `docs/STABLE-SIGNING-RUNBOOK.md`, `docs/REAL-DEVICE-ACCEPTANCE-RUNBOOK.md`, `docs/STABLE-RELEASE-CHECKLIST.md`, and `docs/RELEASE-EVIDENCE-TEMPLATE.md` for execution and evidence capture.

## Promotion rule

Do not change build inputs, application code, Glaze UI resources, signing identity, version metadata, or APK contents after the final signed candidate is accepted. Any such change creates a new candidate and requires the affected validation gates to be repeated.
