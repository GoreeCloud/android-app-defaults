# Native Android Gallery application shell

## Development capability

This milestone adds the first compiled first-party GoreeCloud Gallery Android application module under `native/app`.

The application uses the established `com.goreecloud.gallery` package identity and consumes the already-compiled `android-adapter` MediaStore bridge. It does not reuse the transitional Fossify application architecture or UI.

## Permission and local-media authority

The shell does not query MediaStore before Android grants readable media access. The application now resolves permission state into an explicit access scope rather than a single boolean so partial authority is not presented as full-library authority.

- Android 13+ tracks image and video grants independently.
- Android 14+ also tracks `READ_MEDIA_VISUAL_USER_SELECTED`; selected-media-only authority is presented as **Selected media only**, not as full image/video access.
- When only images or only videos are fully authorized, the UI reports that narrower media type rather than claiming the entire library is readable.
- Android 12L and earlier use the platform read-storage permission within the supported API range.
- Permission authority is re-evaluated when the Activity resumes so a change made through Android permission UI or Settings is not assumed to remain static.
- Selected-media access keeps an explicit **Change selected media** action that returns authority expansion/reselection to Android's permission surface.
- A denied or unavailable provider read is presented as unavailable; the application does not replace provider failure with a false empty-library result.

The current shell reads at most 100 recent rows exposed by the authorized MediaStore view through `AndroidMediaStoreReader`. Android remains authoritative for which rows are visible for the current grant. Gallery does not expand selected-media access in application code.

The shell renders local metadata only and introduces no network permission, cloud dependency, account requirement, analytics, remote font, remote icon, or remote UI resource.

## Glaze UI 2.0 source contract

The shell targets Glaze UI 2.0.0, the current Stable design-system baseline. The first source-level mapping intentionally uses native Android controls and platform theme semantics while preserving Gallery's media-first composition.

The repository-local contract currently enforces:

- Glaze UI version `2.0.0`;
- a 48dp general interactive-target floor;
- adaptive horizontal gutters for phone, tablet, and larger resizable widths;
- native light/dark theme variants;
- local Canvas/Surface-style composition using platform semantic colors;
- no animation dependency for task completion; and
- no network-delivered presentation resources.

This is source integration, not rendered acceptance. Accessibility, large-text reflow, representative phone/tablet/foldable behavior, visual hierarchy, contrast, focus behavior, and physical-device review remain required.

## Current UI scope

The application presents:

- a local-library heading and explicit Development/Glaze source target;
- explicit denied, selected-only, image-only, video-only, image-and-video, or legacy authorized media state as applicable;
- a native permission/reselection action or local refresh action;
- authoritative provider-failure messaging; and
- a bounded newest-first list of authorized image/video metadata including display name, kind, optional album, timestamp, and size.

No thumbnail grid, album navigation, viewer, editor, share flow, delete/move flow, hidden-media policy, Photos integration, or cloud library is implemented by this shell.

## Security, privacy, and continuity boundary

Privacy Shield remains authoritative for media permission, consent, minimization, and user control. This shell relies on Android permission enforcement and does not attempt to bypass Android user/profile isolation.

Wardveil Security remains authoritative for future media/file trust, protection, validation, and security-state presentation. This shell does not claim that local images or videos have been malware-scanned or content-safety classified.

Everkeep remains authoritative for applicable recovery, preservation, portability, and continuity. This shell does not enable Android backup and does not claim backup/restore acceptance.

## Acceptance boundary

This milestone is Development source and build validation only. It does not establish runtime permission behavior on a representative device, MediaStore behavior across supported OEMs/profiles, full selected-media reselection acceptance, rendered Glaze UI acceptance, accessibility acceptance, thumbnail performance, media mutations, release signing, upgrade/recovery evidence, production platform-system acceptance, release, or Stable qualification.
