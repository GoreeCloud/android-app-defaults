# GoreeCloud Gallery Stable Signing Runbook

## Purpose

This runbook defines the controlled administrator procedure for creating, protecting, validating, and using the long-lived GoreeCloud Gallery Android signing identity.

It is an execution guide for issue #2. It does not contain, request, or authorize storage of reusable secret values in Git, issues, pull requests, ordinary documentation, screenshots, logs, or chat.

## Security classification boundary

The following are secret and must remain only in approved protected storage:

- release keystore bytes;
- private signing key material;
- keystore password;
- key password;
- any recovery secret that can unlock or reproduce the signing identity.

The following may be retained as non-secret release evidence after review:

- certificate SHA-256 fingerprint;
- key alias name when disclosure is acceptable;
- creation and verification dates;
- candidate commit and workflow run identifiers;
- APK SHA-256 checksum;
- signing-validation result;
- recovery-copy verification result without secret contents.

Governing rule: document the existence, purpose, owner, lifecycle, and validation of signing secrets without reproducing the active secret values.

## Preconditions

Before creating the long-lived key:

1. Use a trusted GoreeCloud administrative device with current security updates.
2. Confirm the repository and candidate identity are `GoreeCloud/goreecloud-gallery` and `com.goreecloud.gallery`.
3. Confirm `1.0.0-gc.7` remains an Acceptance Candidate until all Stable gates are complete.
4. Confirm at least two approved, independent protected recovery destinations are available.
5. Confirm Vaultwarden or another approved protected credential store is available for passwords, recovery notes, and non-file secret material.
6. Close unrelated terminals and avoid screen sharing, command recording, or logging that could capture secret values.

## Create the release keystore

Use the Android/JDK `keytool` interactively on the trusted administrative device. Do not place passwords directly on the command line.

Example structure:

```bash
umask 077
mkdir -p ~/goreecloud-signing
chmod 700 ~/goreecloud-signing
cd ~/goreecloud-signing
keytool -genkeypair \
  -keystore goreecloud-gallery-release.jks \
  -alias goreecloud-gallery \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
chmod 600 goreecloud-gallery-release.jks
```

The interactive prompts supply secret values. Do not copy the terminal transcript into documentation.

If a different algorithm, alias, validity period, or storage path is selected, record the non-secret rationale in the release evidence without recording secret values.

## Verify the public certificate identity

After creation, derive and record the public SHA-256 certificate fingerprint without exposing the private key:

```bash
keytool -list -v \
  -keystore goreecloud-gallery-release.jks \
  -alias goreecloud-gallery
```

Record only the approved public SHA-256 fingerprint and other non-secret metadata required by the release evidence.

## Create and verify recovery copies

Create at least two independent protected recovery copies. They must not depend on the same device, filesystem, or single failure domain.

For each recovery copy:

1. Copy the keystore through an approved protected method.
2. Preserve restrictive access controls.
3. Record the recovery location by a non-secret stable name, not by publishing credentials or private storage details.
4. Open the copy independently with `keytool -list -v`.
5. Confirm the alias and public SHA-256 fingerprint match the authoritative signing identity.
6. Remove temporary working copies after verification.
7. Record the verification date and result.

A backup is not accepted merely because a file copy exists. At least one recovery copy must be opened and validated before Stable promotion.

## Configure the GitHub `stable-release` environment

Create or review the GitHub environment named `stable-release` and apply the strongest practical protection available for the repository plan.

Configure only these secret names required by the workflow:

- `ANDROID_RELEASE_KEYSTORE_BASE64`
- `ANDROID_RELEASE_KEYSTORE_PASSWORD`
- `ANDROID_RELEASE_KEY_ALIAS`
- `ANDROID_RELEASE_KEY_PASSWORD`
- `ANDROID_RELEASE_CERT_SHA256`

Do not place their active values in repository files, issues, PR bodies, comments, screenshots, or ordinary documentation.

Before entering values into GitHub, verify that `.github/workflows/build-signed-release-candidate.yml` is still manual-only, bound to the `stable-release` environment, and checks out the exact dispatched commit.

## Prepare the keystore value safely

When GitHub requires the keystore as base64, encode it locally without printing unnecessary output to logs or documentation. Use a temporary protected file or direct clipboard transfer appropriate to the administrative device.

The encoded keystore remains secret information. Base64 is encoding, not encryption.

After the environment secret is stored, remove temporary encoded files and clipboard copies where practical.

## Run the signed release-candidate workflow

1. Select the exact reviewed commit intended for the candidate.
2. Confirm ordinary acceptance CI for that exact source line is green.
3. Manually dispatch `.github/workflows/build-signed-release-candidate.yml`.
4. Supply the expected application version exactly as recorded by the candidate.
5. Do not approve Stable publication automatically; the workflow only creates a signed release candidate.

The workflow must pass repository structure/security validation, exact source reconstruction, GoreeCloud source invariants, required behavioral tests, Android lint, release APK assembly, alignment, signing, signer-fingerprint verification, APK validation, and evidence upload.

## Retain non-secret validation evidence

For the signed candidate retain:

- exact repository commit SHA;
- workflow run ID;
- packaged application version;
- source patch line;
- APK SHA-256 checksum;
- approved certificate SHA-256 fingerprint;
- APK validation output;
- behavioral-test evidence;
- lint evidence;
- build-evidence record;
- recovery-copy verification result;
- release classification `signed-release-candidate`.

Do not retain keystore bytes or passwords in the evidence package.

## Rotation and compromise response

Treat the signing identity as compromised if private material or its unlocking secrets enter source control, ordinary documentation, logs, screenshots, chat, email, or another unauthorized location, or if exposure cannot reasonably be ruled out.

If compromise occurs:

1. Stop release use of the affected identity.
2. Preserve incident evidence without copying the secret further.
3. Remove or revoke exposed environment secrets.
4. Determine whether the Android application signing identity can be safely replaced under the distribution model.
5. Create replacement protected material where technically possible.
6. Update consuming release automation.
7. validate the replacement before use.
8. Remove obsolete temporary copies.
9. update the non-secret administrative and recovery records.
10. record the incident and resulting release implications.

Do not assume deleting a leaked file or Git commit restores secrecy.

## Completion criteria for issue #2

Issue #2 may be closed only when all of the following are evidenced:

- the long-lived keystore exists on an approved protected path;
- at least two independent protected recovery copies exist;
- a recovery copy has been opened and verified;
- the public certificate SHA-256 fingerprint is recorded;
- the `stable-release` GitHub environment exists with appropriate protection;
- all required workflow secrets are configured without disclosure;
- the signed release-candidate workflow succeeds from an exact reviewed commit;
- the produced signer matches the approved fingerprint;
- signed-candidate checksum and validation evidence are retained;
- the applicable Stable-release evidence fields are complete.

Completing this runbook does not by itself satisfy real-device acceptance or branch-protection requirements.