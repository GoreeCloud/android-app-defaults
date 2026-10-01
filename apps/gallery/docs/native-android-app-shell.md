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

## Current Glaze source contract

The original shell milestone predates the current Gallery design-system authority. The current native Development line maps to **GLAZE UI V1.6 / 1.6.0**, the Official Anchor shared release. The canonical implementation repository is `GoreeCloud/glaze`; Glaze V1.7 remains Development and is not a consumer baseline for Gallery.

The repository-local contract currently enforces:

- GLAZE UI V1.6 / `1.6.0` as the consumer source baseline;
- a 48dp general interactive-target floor;
- adaptive horizontal gutters for phone, tablet, and larger resizable widths;
- native light/dark theme variants;
- local Canvas/Surface-style composition using platform semantic colors;
- no animation dependency for task completion; and
- no network-delivered presentation resources.

This remains source integration, not whole-application acceptance. Accessibility, large-text reflow, representative phone/tablet/foldable behavior, visual hierarchy, contrast, focus behavior, motion behavior, and physical-device review remain separately gated.

## Current UI scope

The first-party native implementation has advanced substantially beyond the initial shell described by this milestone. Current Development source includes direct **Photos / Albums / Videos / Trash / Settings** navigation, Android-authorized local thumbnail grids, album/favorites browsing, local search and sort, timeline grouping, view-density controls, selection and bulk actions, an authorized media viewer, Android-owned Trash/Restore/Delete flows, Move foundations, and Gallery-local settings.

The October 1, 2026 UI refresh also adds mockup-aligned primary headers, raised album cards, timeline counts, dedicated Trash navigation, destination-specific Videos filters, and cleaner media-first Photos/Videos chrome while grouping and density remain configurable from Settings.

For the maintained current feature inventory and remaining work, use the repository feature/specification records rather than treating this historical shell milestone as the complete application surface.

## Security, privacy, and continuity boundary

Privacy Shield remains authoritative for media permission, consent, minimization, and user control. This shell relies on Android permission enforcement and does not attempt to bypass Android user/profile isolation.

Wardveil Security remains authoritative for future media/file trust, protection, validation, and security-state presentation. This shell does not claim that local images or videos have been malware-scanned or content-safety classified.

Everkeep remains authoritative for applicable recovery, preservation, portability, and continuity. This shell does not enable Android backup and does not claim backup/restore acceptance.

## Acceptance boundary

This milestone is Development source and build validation only. It does not establish runtime permission behavior on a representative device, MediaStore behavior across supported OEMs/profiles, full selected-media reselection acceptance, rendered Glaze UI acceptance, accessibility acceptance, thumbnail performance, media mutations, release signing, upgrade/recovery evidence, production platform-system acceptance, release, or Stable qualification.
