# GoreeCloud Gallery gc.17 Device-Acceptance Fixes

## Purpose

This record documents the representative-device defects that block acceptance of gc.16 and the narrow gc.17 remediation boundary.

## Observed gc.16 defects

Representative-device screenshots identified three release-blocking findings:

1. Contextual and toolbar overflow menus could render a dark Glaze popup surface with foreground text that was too dark to read.
2. The folder-delete confirmation rendered its rounded custom content separately from the AlertDialog action-button area, producing a visually split white-content/black-button surface.
3. Explicitly deleting a selected folder could remove or recycle its media but leave the now-empty folder behind when the general `deleteEmptyFolders` preference was disabled.

The third finding is a functional defect, not merely a visual defect.

## gc.17 remediation

The gc.17 patch keeps the existing Glaze UI 1.0 native mapping and makes the following targeted corrections:

- explicit light and dark popup foreground colors are paired with the established Glaze popup backgrounds;
- both contextual and toolbar overflow popup backgrounds use the same rounded Glaze surface contract;
- the folder-delete content root measures with `wrap_content` and delegates the visible rounded background to the AlertDialog window so content and action buttons form one surface;
- destructive confirmation uses the semantic danger color for the positive action and the semantic accent for cancellation;
- an explicitly selected folder is removed after its deletable media has been removed or moved to the recycle bin when the folder is actually empty, regardless of the incidental `deleteEmptyFolders` preference;
- the Downloads-folder safety exception remains intact;
- a folder that still contains data is not removed by this explicit-empty-folder cleanup path.

## Behavioral boundary

This fix does not weaken Storage Access Framework authorization, locked-folder handling, recycle-bin behavior, Android permission boundaries, Android user/profile isolation, or the no-Internet/privacy posture. It corrects the final empty-folder removal semantics only after the existing authorization and media-operation paths succeed.

## Acceptance requirements

gc.17 must not be promoted solely because source validation passes. The exact PR head must pass repository/security validation, deterministic reconstruction, GoreeCloud behavioral tests, Android lint, APK assembly, APK/evidence validation, and artifact retention.

Representative-device follow-up must then verify at minimum:

- overflow text is readable in both the tested light and dark contexts;
- the destructive confirmation is one coherent rounded surface and remains readable;
- tapping **Yes** on a normal deletable folder actually removes that selected folder after its contents are safely handled;
- **No** remains non-destructive;
- the Downloads safety boundary and non-empty-folder protection remain intact.

## Stable boundary

GoreeCloud Gallery 1.0.0 remains an acceptance candidate. gc.17 does not by itself satisfy the long-lived signing/recovery, repository-protection, broader representative-device permissions/file-operation/user-profile/accessibility, same-signer upgrade/recovery, or final release-evidence gates.
