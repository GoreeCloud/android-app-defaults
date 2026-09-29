# GoreeCloud Gallery Repository Readiness

## Purpose

This document records the source-controlled readiness model for GoreeCloud Gallery. It distinguishes repository readiness from Stable-release readiness and prevents a green build from being interpreted as production approval.

## Current state

- Product: GoreeCloud Gallery
- Current packaged acceptance line: `1.0.0-gc.7`
- Current maintained reconstruction/test patch line: `gc.8`
- Application ID: `com.goreecloud.gallery`
- Repository: `GoreeCloud/goreecloud-gallery`
- Development model: GoreeCloud-maintained open-source Android application based on Fossify Gallery
- License boundary: GNU GPL v3
- User-facing design language: Glaze UI
- Runtime model: offline-first local Android media application
- Release state: Acceptance Candidate
- Stable release: Not approved

The gc.8 layer adds repository/testability and evidence improvements without changing the packaged `1.0.0-gc.7` acceptance identity. Stable classification remains unchanged until all independent release gates are complete.

## Repository readiness gates

### Structure and documentation

- [x] README project entry point.
- [x] Architecture record.
- [x] Build and release model.
- [x] Gallery-specific Glaze UI implementation and release-review contract.
- [x] Release-signing boundary.
- [x] Stable-release checklist.
- [x] Release-evidence template.
- [x] Security policy.
- [x] Contribution guide.
- [x] Modification/upstream provenance notice.
- [x] CODEOWNERS review ownership.
- [x] Pull-request template.
- [x] Dependabot configuration for GitHub Actions.
- [x] Repository structure validator.

### Source provenance

- [x] Exact Fossify Gallery source revision pinned.
- [x] Exact Fossify Commons source revision pinned.
- [x] Accepted gc.1 through gc.7 transformation programs preserved.
- [x] Ordered gc.8 behavioral-test transformation added without rewriting accepted history.
- [x] Patch blob identities validated before execution.
- [x] Source reconstruction refuses ambiguous pre-existing working trees.
- [x] `git diff --check` required on reconstructed source.
- [x] Build evidence records both the exact validated checkout commit and maintained patch line separately from the packaged application version.

### CI and supply-chain controls

- [x] Explicit Ubuntu 24.04 runner.
- [x] Top-level read-only GitHub Actions permissions.
- [x] Third-party Actions pinned to full reviewed commit SHAs.
- [x] Checkout credentials explicitly non-persistent.
- [x] Pull-request validation checks out the exact pull-request head SHA rather than relying on the synthetic merge checkout.
- [x] Push/manual validation checks out the exact GitHub SHA.
- [x] Repository structure validation runs before Android build work.
- [x] Repository security validation runs before Android build work.
- [x] Acceptance workflow has a finite timeout.
- [x] Signed-candidate workflow is manual and bound to `stable-release`.
- [x] Signed-candidate workflow does not publish Stable automatically.

### Behavioral-test evidence controls

- [x] GoreeCloud-specific thumbnail presentation policy is isolated as pure testable behavior.
- [x] GoreeCloud-owned JVM regression tests cover uncropped thumbnails, rounded file thumbnails, and rounded folder thumbnails.
- [x] Acceptance CI fails if the Gradle test task reports `NO-SOURCE`.
- [x] Acceptance CI requires JUnit XML output.
- [x] Acceptance CI requires the GoreeCloud policy test class to have executed.
- [x] Acceptance CI requires at least three executed tests and zero failures/errors.
- [x] Signed-candidate CI enforces the same behavioral-test gate.
- [x] Raw test reports and machine-readable test evidence are retained with build artifacts.
- [x] Exact-head CI has demonstrated 3 executed GoreeCloud tests with 0 failures and 0 errors.

These tests intentionally cover maintainable pure GoreeCloud policy. Permission-sensitive file operations, Android user/profile boundaries, accessibility, lifecycle behavior, and destructive workflows remain real-device acceptance requirements.

### Packaged-artifact controls

- [x] Application ID validation.
- [x] Version validation.
- [x] Debuggable-state validation when expected.
- [x] `android.permission.INTERNET` absence validation.
- [x] APK signature validation.
- [x] Approved certificate-fingerprint validation for signed candidates.
- [x] GoreeCloud notice validation.
- [x] Removed counterfeit-warning DEX validation.
- [x] Final APK SHA-256 generation.
- [x] Build metadata/evidence generation.
- [x] Unit-test evidence retained separately from build success.

## GoreeCloud production-readiness gates

Repository readiness does not waive GoreeCloud production gates.

### Multi-user and private-data boundary

Gallery is a local Android client, so its application-appropriate multi-user model is Android OS user/profile isolation, the Android application sandbox, and permission-authorized media/storage access. It must not deliberately cross or merge private Android user/profile data boundaries.

Production evidence must include a secondary-user or managed/work-profile check when a representative device supports it. A device limitation must be recorded rather than silently treated as a pass.

### Security and safe operation

Security is a lifecycle requirement. Permission-sensitive and destructive media operations require controlled real-device testing with disposable copied media. Passing source/build/test automation does not prove safe file behavior.

### Glaze UI

Glaze UI is mandatory for Gallery's controlled user-facing surfaces. `docs/GLAZE-UI.md` is the repository-local implementation contract. It records product-specific visual invariants, accessibility requirements, privacy/dependency boundaries, upstream-sync review requirements, automated-conformance boundaries, and the exception model.

Current acceptance has validated the previously defective toolbar overflow presentation and automated rounded-thumbnail policy. Stable acceptance still includes broader light/dark, dialog, destructive-action, accessibility, user/profile, text/display scaling, focus, touch-target, and representative-device review. No permanent Glaze UI exception is approved.

## Stable-release blockers

The following work remains intentionally separate from repository-source and automated acceptance readiness:

- **Issue #2 — Android stable signing:** create the long-lived release keystore, maintain independent protected recovery copies, configure the `stable-release` environment secrets, and successfully produce a fingerprint-verified signed candidate.
- **Issue #3 — main branch protection:** enforce repository-level pull-request and acceptance-CI protection so normal direct pushes cannot bypass the validated merge path. The connected source-maintenance capability does not currently expose branch-protection/ruleset mutation, so this remains an explicit repository-administration task rather than being silently assumed complete.
- **Issue #4 — real-device Stable acceptance:** complete storage/permission, copy/move/delete/trash/edit/share, Android user/profile, lifecycle, accessibility, Glaze UI, upgrade, and recovery acceptance using disposable media.
- **Final Stable evidence:** retain the final signed checksum/fingerprint, completed acceptance record, and deliberate publication approval.

The former behavioral-test blocker is satisfied by the gc.8 test foundation and fail-closed JUnit evidence gate. That does not waive any device-dependent acceptance requirement.

Stable must remain blocked while any applicable signing, branch-governance, data-loss, permission, privacy, accessibility, user/profile, upgrade/recovery, licensing, or evidence requirement is incomplete.

## Readiness interpretation

- **Repository ready:** source, governance, build, test, and validation infrastructure are sufficiently structured for controlled ongoing development.
- **Acceptance candidate:** an installable candidate has passed the applicable automated acceptance path but still has blocking release gates.
- **Signed release candidate:** the candidate has passed protected long-lived signing validation but still requires final manual Stable acceptance.
- **Stable:** all blocking release evidence is complete and publication has been deliberately approved.

The current `1.0.0-gc.7` package must continue to be described as an Acceptance Candidate until the remaining Stable-release gates are actually satisfied.
