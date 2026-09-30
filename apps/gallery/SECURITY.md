# GoreeCloud Gallery Security Policy

## Security posture

GoreeCloud Gallery is an offline-first Android gallery application. It is intended to operate against local Android media and storage APIs without analytics, advertising, tracking, cloud accounts, remote APIs, or `android.permission.INTERNET`.

Security and safe operation are release requirements. A successful build does not by itself make a candidate stable.

## Supported line

The current supported development and acceptance line is `1.0.0-gc.7`. It is pre-stable software. Stable publication remains blocked until the signing, real-device, permission, destructive-operation, accessibility, upgrade/recovery, privacy, licensing, and release-evidence gates in `docs/STABLE-RELEASE-CHECKLIST.md` are complete.

## Reporting a vulnerability

Do not publish exploitable security details, reusable secrets, private keys, signing material, authentication tokens, personal media, or other sensitive information in a public issue or pull request.

If GitHub displays a private **Report a vulnerability** option for this repository, use that private reporting path. If it is unavailable, contact the repository owner through an already established private GoreeCloud administrative channel before disclosing exploit details publicly.

A useful report should include, when safe to share:

- affected GoreeCloud Gallery version or commit;
- Android version and device class;
- clear reproduction steps using disposable test media;
- expected and observed behavior;
- security or privacy impact;
- whether the issue can cause unauthorized access, data disclosure, data loss, unsafe file modification, permission bypass, or signing/build compromise;
- any minimal logs or screenshots that do not expose personal media or reusable secrets.

## High-priority security areas

Security review should give particular attention to:

- Android media and storage permissions;
- copy, move, rename, delete, trash/recycle-bin, permanent-delete, and editing flows;
- path and URI handling;
- unintended cross-user or cross-profile access;
- exported Android components and intent handling;
- APK identity and signing integrity;
- dependency and build-workflow integrity;
- accidental network capability;
- secret or signing-key exposure;
- unsafe logging or disclosure of private media metadata.

## Android user and profile boundary

GoreeCloud Gallery does not implement a shared server account system. Its multi-user boundary is the Android operating-system user/profile model and Android application sandbox. The application must not intentionally bypass Android user/profile isolation or combine private media across users or managed profiles without an explicit platform-authorized user action.

Testing this boundary on devices that support secondary users or work profiles is part of production-readiness evidence when practical.

## Release and signing boundary

Stable Android signing material must never be committed to this repository. The long-lived keystore and passwords belong outside Git and ordinary documentation. The manual signed-candidate workflow is isolated behind the `stable-release` GitHub environment and must verify the approved public signing-certificate SHA-256 fingerprint.

See `docs/RELEASE-SIGNING.md` for the signing contract.

## Build supply-chain controls

Repository CI is expected to:

- use explicit least-privilege GitHub Actions permissions;
- check out the exact revision being validated;
- keep checkout credentials non-persistent;
- pin third-party Actions to reviewed full commit SHAs;
- reconstruct exact pinned upstream source;
- fail closed when the historical GoreeCloud patch chain or accepted source shape changes;
- validate the final APK identity, version, permission boundary, signature, notice, DEX warning-removal boundary, and checksum.

## Personal-data testing rule

Security, destructive-operation, and recovery testing must use disposable copied media. Irreplaceable personal photos or videos must not be used as destructive test inputs.

## Disclosure and remediation

Security fixes should be developed through a reviewable branch and pull request, validated against the same release gates as ordinary changes, and documented without publishing reusable secrets. If a security issue affects a published stable release in the future, the remediation record should identify the affected versions, fixed version, validation evidence, and any user action required.