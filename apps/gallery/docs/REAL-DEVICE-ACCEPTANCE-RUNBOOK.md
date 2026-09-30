# GoreeCloud Gallery Real-Device Acceptance Runbook

## Purpose

This runbook defines the representative-device acceptance procedure required before GoreeCloud Gallery can be promoted to Stable.

It operationalizes issue #4, `docs/STABLE-RELEASE-CHECKLIST.md`, `docs/GLAZE-UI.md`, and `docs/RELEASE-EVIDENCE-TEMPLATE.md` without replacing them.

A successful automated build is not sufficient. Stable acceptance requires controlled real-device evidence for safe media operations, Android user/profile boundaries, Glaze UI presentation, accessibility, upgrade continuity, and recovery.

## Test-data boundary

Use only disposable copied media for destructive-operation testing.

Do not use irreplaceable personal photos, videos, documents, or another person's private media as delete/move/edit test inputs.

The evidence record should describe the fixture set generically. Do not commit personal media, private screenshots, user identifiers, or unnecessary device-private information to the repository.

## Candidate identity

Before installation, record:

- exact GoreeCloud repository commit SHA;
- packaged version name and version code;
- source patch line;
- GitHub Actions run ID;
- APK SHA-256 checksum;
- signer certificate SHA-256 fingerprint when testing a signed candidate;
- device model;
- Android version and relevant security patch level;
- test date.

Verify the APK being installed matches the recorded checksum.

## Disposable fixture preparation

Prepare a small copied fixture set that exercises representative behavior without exposing private information. Include where practical:

- JPEG/PNG images;
- a short video;
- nested folders;
- duplicate filenames in different folders;
- files intended for rename/copy/move/delete;
- a file intended for image-edit save testing;
- a hidden/excluded-folder case when supported;
- trash/recycle-bin test files.

Record only fixture categories and outcomes, not unnecessary file contents.

## Fresh-install and permission acceptance

1. Install the exact candidate on the representative device.
2. Launch from a clean application state.
3. Confirm startup completes without unexplained crash or network/account requirement.
4. Exercise Android media/storage permission prompts.
5. Deny optional permissions and confirm the application fails safely and explains blocked capability where appropriate.
6. Grant required permissions and confirm intended functionality becomes available.
7. Revoke a granted permission from Android Settings.
8. Relaunch Gallery and confirm permission loss is handled safely.
9. Confirm the application does not request or depend on network access.
10. Verify scoped-storage or storage-access-framework flows required by the Android version.

## Android user/profile isolation

Gallery's application-appropriate multi-user boundary is the Android OS user/profile model, application sandbox, permission framework, and platform-authorized media/storage access.

On a device that supports a secondary user or managed/work profile:

1. Prepare distinct disposable media in each available profile boundary.
2. Launch Gallery from the tested profile.
3. Confirm media from another Android user/profile is not silently merged or exposed.
4. Confirm any cross-profile behavior requires an explicit platform-authorized user action.
5. Confirm application settings/data remain appropriately isolated by Android.

If the device cannot provide a secondary/managed profile, record that limitation explicitly. Do not mark the profile gate passed without evidence.

## Core media behavior

Using disposable fixtures, verify:

- browse folders and media;
- open images;
- open/play videos;
- search;
- mark and unmark favorites;
- copy;
- move;
- rename where supported;
- delete;
- restore from trash/recycle bin where supported;
- permanently delete disposable media where supported;
- hidden/excluded folder behavior;
- image editing and save behavior;
- Android share/open-with chooser behavior;
- rotation/background/return behavior during safe operations.

After each destructive or mutating operation, verify the actual filesystem/media result rather than relying only on the UI toast or list state.

Any unexpected data loss, corruption, duplicate destructive action, or operation against the wrong target is a Stable blocker.

## Glaze UI acceptance

Glaze UI is mandatory for controlled Gallery surfaces. No permanent Gallery exception is approved.

Review the main folders screen, opened folder/media grid, search surfaces, overflow menus, dialogs, settings, selection/action modes, image viewer/editor surfaces, trash/recycle interfaces, permission/error states, and other user-facing surfaces reached during the test.

Verify in both light and dark appearance where supported:

- GoreeCloud identity is correct and residual upstream product branding does not misrepresent the app;
- rounded geometry is consistent on thumbnails, dialogs, controls, cards, and other intended surfaces;
- layered/translucent presentation is used selectively without reducing readability;
- shadows and gradients remain restrained and purposeful;
- foreground/background contrast is readable;
- overflow menus are readable;
- dialog foreground/background contrast is readable;
- destructive actions are visually distinguishable and require intended confirmation;
- selection states are clear;
- controls do not overlap, clip, or become unreachable;
- screen rotation and different display sizes do not expose obviously unfinished upstream styling;
- decorative effects do not reduce task completion, responsiveness, or legibility.

Temporary development divergence is not a Stable exception. Any material Glaze UI noncompliance must be fixed or explicitly documented under the GoreeCloud exception standard before production approval.

## Accessibility acceptance

Test with representative accessibility settings and record outcomes:

1. Enable TalkBack and navigate primary screens and dialogs.
2. Confirm actionable icon-only controls have meaningful accessible labels where required.
3. Confirm focus order is usable and does not trap the user.
4. Increase system font size and verify critical text/actions remain available.
5. Increase display size and verify core flows remain usable.
6. Confirm practical touch targets for frequently used controls.
7. Verify light/dark contrast for normal, disabled, selected, destructive, dialog, and menu states.
8. Confirm motion/animation does not block task completion; test reduced-motion behavior where the platform/application exposes it.

An accessibility defect that prevents a core task or makes a destructive confirmation ambiguous is a Stable blocker.

## Upgrade acceptance

Upgrade testing must use builds signed with the same approved long-lived release certificate.

1. Install the prior approved candidate/stable build.
2. Exercise representative settings and media views.
3. Record the prior version and signer fingerprint.
4. Upgrade in place to the new candidate without clearing application data.
5. Confirm the new version starts normally.
6. Confirm expected application settings persist.
7. Confirm local media is unchanged by the application upgrade.
8. Confirm favorites/hidden/excluded state expected to persist behaves correctly.
9. Confirm the signing certificate remains the approved identity.
10. Repeat representative browse/open/search and a safe mutation operation after upgrade.

Do not treat uninstall/reinstall as a successful in-place upgrade test.

## Bad-release recovery acceptance

Android normally prevents ordinary downgrade to a lower version code. Recovery must therefore be tested as a deliberate procedure rather than assumed package rollback.

Document and verify the recovery path appropriate to the candidate, including:

- how a bad release would be stopped from further distribution;
- whether a corrected higher-version build is the preferred recovery;
- whether uninstall/reinstall would reset application-specific state;
- which application settings would require restoration or reconfiguration;
- confirmation that personal media is not treated as application-private rollback state;
- confirmation that media remains independently protected outside application rollback.

If a recovery procedure requires destructive application-data removal, document the user-visible consequence before Stable approval.

## Evidence capture

Complete `docs/RELEASE-EVIDENCE-TEMPLATE.md` using non-secret, privacy-safe evidence.

For each acceptance area record:

- pass/fail/not-applicable/blocked;
- device/Android context where relevant;
- concise observed result;
- defect or issue reference when failed;
- screenshot reference only when the image contains no personal/private material or secrets.

Do not silently convert an untested item into pass.

## Stop conditions

Stop Stable promotion and open/fix a defect if testing finds:

- data loss or corruption;
- wrong-target delete/move/edit behavior;
- Android user/profile privacy boundary violation;
- unsafe permission behavior;
- unexpected network dependency;
- crash in a core flow;
- unreadable or materially noncompliant Glaze UI surface;
- inaccessible destructive confirmation or core task;
- failed in-place upgrade;
- signer mismatch;
- recovery path that cannot reasonably preserve user media and required state;
- another unresolved P0/P1 release defect.

Retest the affected gate on a newly reviewed candidate after correction.

## Completion criteria for issue #4

Issue #4 may be closed only when the representative-device evidence covers all applicable blocking areas, limitations are explicitly recorded, no unresolved data-loss/privacy/security/accessibility/upgrade/recovery/Glaze UI blocker remains, and the completed evidence identifies the exact candidate being approved.

Completing this runbook does not itself satisfy signing or repository branch-protection requirements.