# GoreeCloud Gallery Release Signing

## Purpose

This document defines the release-signing boundary for GoreeCloud Gallery.

Android uses the application signing certificate as part of application identity. A stable GoreeCloud Gallery release must therefore use one long-lived GoreeCloud-controlled signing key that is protected outside source control and reused for every compatible future release.

The current `1.0.0-gc.7` acceptance APK is not the stable release-signing baseline.

## Security requirements

The release keystore, keystore password, key password, private key material, recovery copies, and any reusable signing credentials must never be committed to this repository, written into workflow files, placed in issue or pull-request comments, or included in build artifacts.

The signing key must be backed up independently before it is used to publish the first stable application release. Losing the stable signing key can prevent normal in-place upgrades. Replacing the signing identity can also break the Android upgrade relationship with previously installed stable builds.

## GitHub Actions secret contract

The manual `.github/workflows/build-signed-release-candidate.yml` workflow expects the following secrets in the `stable-release` GitHub environment:

- `ANDROID_RELEASE_KEYSTORE_BASE64` — base64-encoded keystore bytes.
- `ANDROID_RELEASE_KEYSTORE_PASSWORD` — keystore password.
- `ANDROID_RELEASE_KEY_ALIAS` — alias of the approved signing key.
- `ANDROID_RELEASE_KEY_PASSWORD` — password for the selected key.
- `ANDROID_RELEASE_CERT_SHA256` — SHA-256 fingerprint of the approved signing certificate.

The workflow fails closed when any required value is missing. It reconstructs the pinned GoreeCloud source, runs the release validation path, builds the unsigned FOSS release APK, aligns it, signs it with `apksigner`, verifies the signature, verifies the certificate fingerprint, verifies the package identity and version, verifies that `android.permission.INTERNET` is absent, scans the packaged DEX files for the removed counterfeit-build warning, and publishes only the signed release-candidate artifact and validation evidence.

## GitHub environment protection

Create or review a GitHub environment named `stable-release` before configuring signing secrets.

Recommended controls:

- restrict the environment to the intended release branch or tag policy;
- require deliberate approval before a signing job can access the environment secrets when the repository/account plan supports that control;
- keep environment secrets scoped only to the signing workflow;
- do not expose signing secrets to pull-request workflows;
- review the workflow diff before approving any change that can access the signing environment.

The ordinary acceptance workflow uses read-only repository permissions and does not receive release-signing secrets.

## Creating the long-lived key

Key creation is intentionally an administrator-controlled operation outside GitHub Actions. Generate the keystore on a trusted administrative device with a current Java `keytool` or another Android-compatible key-management method.

A typical local command shape is:

```bash
keytool -genkeypair \
  -keystore goreecloud-gallery-release.jks \
  -alias goreecloud-gallery \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Do not reuse example passwords, and do not paste the resulting private values into documentation or chat records.

After creation, record the public certificate fingerprint separately from the private key:

```bash
keytool -list -v \
  -keystore goreecloud-gallery-release.jks \
  -alias goreecloud-gallery
```

The SHA-256 certificate fingerprint becomes the approved `ANDROID_RELEASE_CERT_SHA256` value used by CI to prevent accidental signing with a different certificate.

## Backup and recovery

Before the first stable release:

1. create at least two protected copies of the keystore in approved, independent locations;
2. store the associated passwords and recovery information in the approved GoreeCloud sensitive-information system rather than ordinary documentation;
3. verify that a recovery copy can be opened and that the expected alias and certificate fingerprint are present;
4. document the recovery location without recording the reusable secret values themselves;
5. treat signing-key recovery as part of the stable-release readiness evidence.

## Release-candidate signing flow

The signing workflow is manual by design. It does not publish a GitHub Release and does not promote an APK to stable automatically.

A signed candidate becomes eligible for stable promotion only after the manual device, permission, destructive-operation, accessibility, upgrade/recovery, licensing, and release-evidence gates in `STABLE-RELEASE-CHECKLIST.md` are complete.

## Key rotation

Do not rotate the stable signing key as routine maintenance. Android signing-key changes require a deliberate compatibility and migration plan. If compromise is suspected, stop release publication, preserve evidence, revoke access to the signing environment, assess installed-version impact, and plan the supported Android signing-key transition before publishing another build.
