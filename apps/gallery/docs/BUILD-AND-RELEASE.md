# GoreeCloud Gallery Build and Release Model

## Purpose

This document defines the reproducible build, validation, acceptance, signing, evidence-retention, and stable-promotion model for GoreeCloud Gallery.

GoreeCloud Gallery is a GoreeCloud-maintained Android application based on Fossify Gallery. The installable application ID is `com.goreecloud.gallery`.

## Pinned upstream source

The gc.7 line is built from exact source revisions:

- Fossify Gallery 1.13.1: `b28299dc33821eee8d108a9880ce87876cf31443`
- Fossify Commons 6.1.5: `acfd352df1a1852d17a5f77def8b7ad6e522a5b6`

The reconstruction process checks out those revisions in detached state and refuses to treat another revision as the accepted baseline.

## Migrated patch provenance

The original gc.1 through gc.7 patch scripts were developed on the isolated `build/goreecloud-gallery-apk` branch of `GoreeCloud/goreecloud-website`.

During the dedicated-repository migration, each historical script was preserved as ordered source fragments under `patches/gc1/` through `patches/gc7/`. `scripts/materialize-patches.sh` reconstructs each script under `.build/patches/` and validates its Git blob SHA against the exact historical source blob before the script can execute.

This fail-closed provenance check prevents an incomplete migration, missing fragment, changed byte sequence, or accidental rewrite from silently becoming a build input.

Historical script blobs:

- gc.1 — `100b2080cf5e82275dce7a1a1f35d8869ab8af38`
- gc.2 — `5a9d84b0eaa49107cba52f2b1f02131fa5d03f3e`
- gc.3 — `67a4e4acfebb2c6e3d271d5387ededa60bc0ee87`
- gc.4 — `8f3a48d424ead1c253e0e9eb91f27706e6162757`
- gc.5 — `e8b1362e87d0e47997fa7f2ee36f851b4123fab5`
- gc.6 — `4c9094e7b4139e0472f1c17ab2ff4a2186244c78`
- gc.7 — `516339487492806932ec14b669c183e4919b1187`

Accepted historical patch programs are provenance. New product behavior should normally extend the ordered GoreeCloud patch line rather than silently rewriting an already accepted historical transformation.

## Source reconstruction

`scripts/reconstruct-source.sh` is the authoritative CI reconstruction entry point for the gc.7 line. It:

1. refuses to reuse an existing Gallery or Commons working directory;
2. materializes and verifies the historical GoreeCloud patch programs;
3. clones the exact upstream repositories;
4. checks out the exact accepted Gallery and Commons commits;
5. applies gc.1 through gc.7 in order;
6. normalizes the accepted `settings.gradle.kts` terminal-newline state;
7. runs `git diff --check` against both reconstructed trees.

The script exists so acceptance and signed-release-candidate workflows do not carry separate, potentially drifting copies of the source reconstruction process.

## Patch history

The deterministic patch chain is cumulative:

1. **gc.1** — GoreeCloud package identity, branding, initial Glaze palette, launcher identity, offline boundary, and license notice.
2. **gc.2** — local Fossify Commons composite build, system-theme correction, launcher aliases, and maintained-fork identity fixes.
3. **gc.3** — real-device identity corrections, remaining Compose counterfeit-warning boundary, canonical launcher behavior, and Glaze app-bar surfaces.
4. **gc.4** — rounded Glaze Settings cards and popup/dialog geometry refinement.
5. **gc.5** — legacy non-Compose counterfeit-warning removal, popup contrast correction, rounded thumbnail defaults, API-qualified navigation-bar resources, and expanded readiness validation.
6. **gc.6** — complete removal of square-thumbnail controls from the GoreeCloud product surface, forced rounded folder/media thumbnails, and first toolbar overflow correction attempt.
7. **gc.7** — direct correction of the real owner path for the remaining overflow defect: Fossify Commons `MySearchMenu` and its embedded `MaterialToolbar`.

## Glaze UI contract

The Android implementation emphasizes rounded containers and media surfaces, layered light/dark surfaces, restrained depth, readable popup/dialog contrast, touch-friendly spacing, GoreeCloud-controlled product identity, and accessibility over decorative effects.

Square thumbnail presentation is intentionally not exposed. File and folder thumbnails resolve to the rounded GoreeCloud presentation even when imported or legacy preferences previously selected square behavior.

Toolbar overflow menus are also a Glaze-controlled surface. A light GoreeCloud screen must not display a dark inherited popup and popup foreground/background colors must remain readable.

Decorative translucency, gradients, or depth never override readability, accessibility, performance, or reliable media interaction.

## Offline, user, and privacy boundary

GoreeCloud Gallery is intended to operate entirely against local Android media and storage APIs. The GoreeCloud patchset does not add analytics, advertising, tracking, cloud accounts, remote APIs, or `android.permission.INTERNET`.

Gallery is a local Android client rather than a shared GoreeCloud server. Its application-appropriate multi-user boundary is Android OS user/profile isolation, the application sandbox, permission controls, and platform-authorized media/storage access. Gallery must not deliberately cross Android user/profile boundaries or combine private media across users or managed profiles without an explicit platform-authorized action.

`scripts/validate-apk.sh` requires Android `apkanalyzer` and verifies the packaged application ID, version, Internet-permission boundary, optional expected debuggable state, valid APK signature, optional expected signer-certificate fingerprint, GoreeCloud notice, removed-warning DEX boundary, and final SHA-256 checksum.

Android storage-management permissions are separate from network access and remain subject to real-device acceptance because file-management permissions are security-sensitive.

## Repository readiness validation

Before Android source reconstruction begins, CI validates the GoreeCloud repository itself.

`scripts/validate-repository-structure.sh` verifies required project/governance records, historical patch directories, license/provenance markers, architecture/readiness boundaries, and that generated build or key-container material is not tracked.

`scripts/validate-repository-security.sh` validates shell syntax, explicit read-only workflow permissions, absence of `pull_request_target`, immutable third-party Action SHAs, explicit non-persisted checkout credentials, exact-revision checkout requirements, manual `stable-release` isolation for signed candidates, and common signing/private-key file boundaries.

These checks protect the build process itself rather than assuming a successful Android compile proves repository safety.

## Acceptance CI

`.github/workflows/build-and-validate.yml` is the ordinary pull-request and `main` acceptance workflow.

For a pull request, the workflow checks out `github.event.pull_request.head.sha` explicitly. For push/manual execution it checks out `github.sha`. It then verifies `git rev-parse HEAD` equals that expected revision before any build work. This prevents validation of GitHub's synthetic pull-request merge commit from being confused with validation of the exact proposed source revision.

The workflow performs:

- exact-revision checkout verification;
- repository structure validation;
- repository security validation;
- exact source reconstruction through `scripts/reconstruct-source.sh`;
- centralized source acceptance assertions through `scripts/validate-source-invariants.sh`;
- Android unit-test task execution and retained console evidence;
- explicit `NO-SOURCE` interpretation when applicable;
- Android lint;
- FOSS debug APK compilation;
- packaged application-ID verification;
- packaged version verification;
- packaged Internet-permission verification;
- APK signature verification;
- expected debug-state verification;
- packaged DEX scanning for removed counterfeit-warning text;
- GoreeCloud notice verification;
- GNU GPL license preservation in the produced artifact bundle;
- SHA-256 generation;
- machine-readable build identity through `scripts/write-build-evidence.sh`;
- retention of APK validation output, unit-test interpretation, available test reports, lint output, and Gradle problem reports;
- GitHub Actions artifact upload.

The workflow uses read-only repository permissions, disables persisted checkout credentials, runs on explicit Ubuntu 24.04, and pins third-party GitHub Actions to reviewed commit SHAs rather than mutable version tags.

Dependabot is configured for the `github-actions` ecosystem so updates to the pinned Actions arrive as reviewable pull requests instead of being adopted implicitly.

### Unit-test coverage interpretation

The upstream `:app:testFossDebugUnitTest` task has reported `NO-SOURCE`. That is a successful Gradle task result but it is not meaningful automated behavioral test coverage.

The acceptance artifact now records this state explicitly in `dist/validation/unit-test-coverage.txt`. If the task stops reporting `NO-SOURCE`, the evidence instructs reviewers to inspect the retained reports rather than automatically claiming a specific coverage level.

Targeted GoreeCloud-owned behavioral tests remain a release-readiness requirement tracked separately where they can be added without creating disproportionate fork-maintenance cost.

## Machine-readable build evidence

`scripts/write-build-evidence.sh` records non-secret build identity for the final APK, including:

- product and application ID;
- GoreeCloud version and release classification;
- exact repository commit;
- exact pinned Fossify Gallery and Commons commits;
- GitHub workflow run metadata when available;
- APK filename and SHA-256;
- expected absence of Internet permission;
- Glaze UI requirement;
- Android OS user/profile and application-sandbox multi-user model;
- the fact that Stable publication is not automatic.

The build-evidence file supplements the final APK validator output. It does not replace real-device or stable-promotion acceptance.

## Signed release-candidate workflow

`.github/workflows/build-signed-release-candidate.yml` is manual and isolated from ordinary pull-request CI.

The workflow checks out the exact dispatched `github.sha`, verifies the checked-out revision, validates repository structure/security, reconstructs the same pinned source, validates the same source invariants, runs the release unit-test/lint path with retained `NO-SOURCE` interpretation, builds the FOSS release APK, verifies that all required signing secrets are present, aligns the APK, signs it with Android `apksigner`, and validates the final signed package with `scripts/validate-apk.sh`.

The workflow expects signing values only from the `stable-release` GitHub environment and verifies that the produced APK is signed by the approved long-lived certificate through `ANDROID_RELEASE_CERT_SHA256`.

The workflow does not run automatically on push or pull requests and does not publish a GitHub Release. Its output is a signed release candidate plus checksum, licensing/notice files, build metadata, and available validation evidence. Stable publication remains a separate deliberate decision after manual acceptance.

See `RELEASE-SIGNING.md` for the full key-management and GitHub secret contract.

## Real-device acceptance

The gc.6 screenshots established that the no-square-thumbnail work and standard dialogs behaved as intended, but the top-bar three-dot menus still inherited a dark popup with unreadable dark foreground text.

gc.7 moved the correction into `MySearchMenu`, assigning a GoreeCloud light or dark popup theme directly to the embedded `MaterialToolbar` during construction and whenever search-bar colors refresh.

Real-device screenshots supplied after gc.7 confirm that the previously defective overflow menus are now readable on both the main folders view and an opened media folder. This closes that specific visual acceptance defect.

That acceptance does not by itself prove all file-management, storage-permission, Android user/profile isolation, accessibility, signing, automated-behavior, or upgrade/recovery requirements.

`STABLE-RELEASE-CHECKLIST.md` is the release gate for those remaining areas. Destructive operation testing must use disposable copied media rather than irreplaceable personal data.

## Stable release signing

A stable Android application signing identity must be long lived, independently backed up, and protected outside source control.

Before the first stable release:

- create the GoreeCloud Gallery release key on a trusted administrative device;
- protect the keystore and passwords outside Git and ordinary documentation;
- retain independent recovery copies;
- verify a recovery copy before first publication;
- configure the required secrets only in the `stable-release` GitHub environment;
- record the public SHA-256 certificate fingerprint as release evidence;
- successfully produce and validate a signed release candidate.

The current gc.7 acceptance APK must not be treated as the long-term stable signing baseline merely because it installs successfully.

## Upgrade and recovery model

Stable candidates must be upgrade-tested from the previous approved build signed with the same release identity.

Android normally prevents an in-place downgrade to a lower version code. A bad-release rollback therefore requires a documented recovery path rather than an assumption that the package manager will accept an older APK directly. GoreeCloud Gallery primarily operates on local user media, so release testing must verify that application upgrade/recovery actions do not damage or become the sole protection for personal media.

## Release evidence

For every candidate considered for stable promotion, use `RELEASE-EVIDENCE-TEMPLATE.md` and retain at minimum:

- GoreeCloud commit SHA;
- upstream Gallery and Commons SHAs;
- version name and version code;
- workflow run identifier;
- final signed APK SHA-256;
- signer certificate SHA-256;
- build metadata and APK validation evidence;
- explicit unit-test coverage interpretation;
- lint/report evidence;
- test device and Android version;
- Android user/profile isolation result where the device supports it;
- real-device permission and destructive-operation acceptance result;
- accessibility acceptance result;
- upgrade/recovery acceptance result;
- license and provenance review result.

## Stable-promotion gate

Before stable promotion, complete every blocking item in `STABLE-RELEASE-CHECKLIST.md`.

At minimum, stable release remains blocked until:

- the long-lived release signing key and recovery copies exist;
- the protected signing environment is configured;
- a signed release candidate passes CI and signer-fingerprint verification;
- broader real-device copy, move, delete, recycle-bin/trash, hidden/excluded, favorites, playback, editing, and destructive-operation flows pass;
- storage-permission behavior passes;
- Android user/profile isolation is accepted where representative device support exists or the test limitation is explicitly recorded;
- upgrade and recovery behavior passes;
- accessibility review passes;
- meaningful GoreeCloud-owned behavioral-test evidence is established where maintainable or the remaining manual-only boundary is explicitly justified;
- final license/source attribution passes;
- final release evidence is retained;
- repository-level `main` protection is configured so the normal acceptance path cannot be bypassed.

The current gc.7 acceptance APK is not a stable production release.
