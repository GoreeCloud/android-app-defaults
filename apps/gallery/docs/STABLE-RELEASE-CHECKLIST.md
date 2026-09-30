# GoreeCloud Gallery Stable Release Checklist

## Purpose

This checklist defines the minimum evidence required before GoreeCloud Gallery can be promoted from an acceptance or signed release candidate to a stable release.

A green build alone is not stable-release approval. Stable promotion requires both automated evidence and controlled real-device acceptance.

Use `docs/RELEASE-EVIDENCE-TEMPLATE.md` to record the candidate evidence without placing reusable secrets or personal media in the record.

## Release identity

Record the following for every candidate:

- [ ] GoreeCloud repository commit SHA.
- [ ] GoreeCloud version name and version code.
- [ ] Fossify Gallery upstream commit SHA.
- [ ] Fossify Commons upstream commit SHA.
- [ ] GitHub Actions run identifier.
- [ ] APK SHA-256 checksum.
- [ ] Signing-certificate SHA-256 fingerprint.
- [ ] Test device model and Android version.
- [ ] Date and result of real-device acceptance.

## Repository, source, and build gates

The candidate must pass all applicable CI gates without bypassing a failed step:

- [ ] Repository structure validation passes.
- [ ] Repository security guardrails pass.
- [ ] CI validates the exact repository revision under review rather than a different or synthetic revision.
- [ ] Patch fragments reconstruct to the exact historical Git blob identities.
- [ ] Pinned upstream Gallery and Commons commits are checked out exactly.
- [ ] The gc.1 through gc.7 patch chain applies without source-shape exceptions.
- [ ] `git diff --check` passes for both reconstructed repositories.
- [ ] GoreeCloud source acceptance invariants pass.
- [ ] Android unit-test task executes successfully. If the task reports `NO-SOURCE`, record that automated unit-test coverage is absent rather than treating it as test coverage.
- [ ] Android lint task completes and its report is retained with release evidence.
- [ ] Release APK builds successfully.
- [ ] APK signature verifies with `apksigner`.
- [ ] APK signer certificate matches the approved long-lived GoreeCloud release certificate.
- [ ] Application ID is exactly `com.goreecloud.gallery`.
- [ ] Version name matches the candidate being reviewed.
- [ ] Release APK is not debuggable.
- [ ] `android.permission.INTERNET` is absent from the packaged APK.
- [ ] Removed counterfeit-build warning text is absent from packaged DEX files.
- [ ] GoreeCloud notice is present and matches the candidate version.
- [ ] GNU GPL v3 license evidence is included with the release artifact.
- [ ] Machine-readable build evidence identifies the exact GoreeCloud and upstream source revisions.
- [ ] SHA-256 checksum is generated from the final signed APK.

## Signing-key and recovery gates

Complete these before the first stable release and revalidate when the signing boundary changes:

- [ ] Long-lived GoreeCloud Gallery release keystore has been created on a trusted administrative device.
- [ ] Keystore is not stored in Git or ordinary documentation.
- [ ] Required signing values are configured only in the protected `stable-release` GitHub environment.
- [ ] At least two independent protected recovery copies of the keystore exist.
- [ ] A recovery copy has been opened successfully and the alias/fingerprint verified.
- [ ] Signing-certificate SHA-256 fingerprint recorded as non-secret release evidence.
- [ ] Signing workflow cannot access secrets from ordinary pull-request validation.

## Android user/profile isolation acceptance

GoreeCloud Gallery is a local Android client rather than a shared server application. Its application-appropriate multi-user boundary is Android OS user/profile isolation, the application sandbox, permission controls, and platform-authorized media/storage access.

- [ ] The candidate does not intentionally bypass Android user/profile isolation.
- [ ] The candidate does not combine private media across Android users or managed/work profiles without an explicit platform-authorized user action.
- [ ] On a representative device that supports a secondary user or managed/work profile, Gallery behavior is checked from the available profile boundary.
- [ ] If the available test device cannot create a secondary or managed profile, the limitation is recorded rather than silently marked passed.

## Storage and permission acceptance

Use a real device and disposable test media. Do not use irreplaceable personal media as destructive-operation test input.

- [ ] Fresh install starts successfully.
- [ ] Expected Android media/storage permissions are requested only when needed.
- [ ] Denying optional permissions does not create an unexplained crash or unsafe state.
- [ ] Granting required permissions enables the intended feature.
- [ ] Permission removal from Android Settings is handled safely on next launch.
- [ ] The application does not request network access.
- [ ] Scoped-storage or storage-access-framework paths behave correctly on the tested Android version.

## Media and file-operation acceptance

Test against copied/disposable files and verify both the UI result and the actual filesystem/media result where practical.

- [ ] Browse folders and media.
- [ ] Open images.
- [ ] Open and play videos.
- [ ] Search media.
- [ ] Mark and unmark favorites.
- [ ] Copy media.
- [ ] Move media.
- [ ] Rename media where supported.
- [ ] Delete media.
- [ ] Restore from recycle bin/trash where supported.
- [ ] Permanently delete disposable test media where supported.
- [ ] Hidden/excluded folder behavior works as intended.
- [ ] Image editing saves the intended result without corrupting the source unexpectedly.
- [ ] Share/open-with flows invoke the expected Android chooser without requiring GoreeCloud network access.
- [ ] Rotation, backgrounding, and returning to the application do not lose or corrupt an in-progress safe operation.

## Glaze UI and visual acceptance

- [ ] Main folder view follows the accepted Glaze UI presentation.
- [ ] Opened media-folder view follows the accepted Glaze UI presentation.
- [ ] File and folder thumbnails remain rounded; removed square-thumbnail controls do not reappear.
- [ ] Three-dot toolbar overflow menus remain readable in light mode.
- [ ] Three-dot toolbar overflow menus remain readable in dark mode.
- [ ] Dialog foreground/background contrast is readable.
- [ ] Destructive actions are visually distinguishable and require the intended confirmation behavior.
- [ ] No residual Fossify product branding incorrectly presents the application as the upstream product.
- [ ] Decorative translucency, gradients, or depth do not reduce readability, performance, or task completion.

## Accessibility acceptance

- [ ] TalkBack can identify primary navigation and actionable controls.
- [ ] Icon-only controls have meaningful accessible labels where required.
- [ ] Large font size does not hide critical controls or confirmation text.
- [ ] Increased display size does not make core flows unusable.
- [ ] Interactive controls have practical touch targets.
- [ ] Light and dark themes preserve readable contrast for text, menus, dialogs, and disabled states.
- [ ] Focus order is usable for major screens and dialogs.
- [ ] Motion or animation does not block task completion.

## Upgrade and recovery acceptance

Android normally prevents an in-place downgrade to a lower version code. Treat rollback as a controlled recovery procedure rather than assuming a normal package downgrade will work.

- [ ] Install the prior approved candidate/stable build signed with the same release certificate.
- [ ] Exercise representative settings and media views.
- [ ] Upgrade in place to the new candidate.
- [ ] Confirm application settings expected to persist remain valid.
- [ ] Confirm local media is unchanged by the application upgrade.
- [ ] Confirm the signing certificate remains the approved stable certificate.
- [ ] Verify the documented recovery path for a bad release, including whether uninstall/reinstall would reset application-specific settings.
- [ ] Confirm that personal media is not used as application-private rollback state and remains independently protected.

## Privacy and security review

- [ ] No analytics SDK, advertising SDK, telemetry service, cloud account requirement, remote API, or tracking integration was introduced.
- [ ] No reusable secret, private key, token, keystore, or password is present in Git history for the candidate changes.
- [ ] GitHub Actions use least-privilege permissions.
- [ ] Third-party GitHub Actions are pinned to reviewed commit SHAs.
- [ ] Checkout credentials are not persisted when they are unnecessary.
- [ ] Pull-request validation does not use `pull_request_target` for untrusted source validation.
- [ ] Signed-candidate automation remains manual and isolated behind the protected `stable-release` environment.
- [ ] Build artifacts contain only intended APK, checksum, license/notice, and validation evidence.
- [ ] Destructive file operations have been tested only with disposable media before stable promotion.

## License and provenance review

- [ ] Root GNU GPL v3 license text remains present.
- [ ] Upstream GNU GPL v3 license remains preserved in reconstructed source and release evidence.
- [ ] GoreeCloud modification notice is present.
- [ ] Upstream project and exact source revisions are documented.
- [ ] GoreeCloud patch history is preserved and reproducible.
- [ ] Source corresponding to the distributed build can be reconstructed from the repository and pinned upstream revisions.

## Final stable-promotion decision

Stable promotion is allowed only when:

- [ ] all blocking checklist items are complete;
- [ ] no known P0/P1 release defect remains open;
- [ ] no unresolved data-loss, destructive-operation, user/profile-isolation, permission, privacy, signing, accessibility, upgrade/recovery, or licensing defect remains open;
- [ ] final signed APK checksum and signer fingerprint are recorded;
- [ ] final release evidence is retained;
- [ ] the release is deliberately approved for publication.

If any blocking item fails, keep the build classified as an acceptance or release candidate, correct the issue, rebuild from a new reviewed commit, and repeat the affected acceptance gates.
