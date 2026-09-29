# GoreeCloud Gallery — Implemented Features

## 2026-09-29 — bounded authorized local search

Development source now routes rendered Gallery search through one bounded core contract over the already-authorized visible media snapshot. Search tokenizes case-insensitively across display name, album name, MIME type, and image/video kind, preserves snapshot order before existing presentation sorting, and caps results at 100. Photos, Videos, Favorites, opened albums, and album collection derivation all use the same contract. Search does not issue MediaStore queries, inspect media bytes, persist search history, or add network/storage authority.


### Local browsing presentation candidate

The current Gallery candidate exposes three persisted media-grid densities: **Dense**, **Comfortable**, and **Spacious**. Spacious reduces the adaptive baseline by two columns while preserving a minimum two-column grid, so the setting remains usable on narrow screens. The setting stays presentation-only and participates in the existing non-secret settings portability path.

## September 29, 2026 — persistent local sort-order candidate

The active Gallery browsing candidate now persists **Newest first / Oldest first** as an app-private presentation preference instead of resetting sort order on every launch. The existing header sort action writes the preference, Settings exposes the same choice explicitly, and Gallery settings export/import carries the portable `sortPreference` value. The preference maps only to the existing core `MediaSortOrder` and composes with Day / Month / Year / None grouping over the same Android-authorized media snapshot.

Newest remains the migration-safe default. This adds no MediaStore mutation, permission, account, network, synchronization, or storage authority. Pure policy coverage locks defaulting, stored-value round trips, and mapping to the existing core sort contract. Representative-device/adaptive/accessibility acceptance remains open.

## September 29, 2026 — local timeline grouping candidate

The current Gallery candidate adds a persisted local **Group media by** presentation preference with **Day**, **Month**, **Year**, and **None** modes. Day remains the migration-safe default so existing installs keep Today/Yesterday/calendar-day sections. Month and Year group the same already-authorized media into broader calendar sections, while None shows the same ordered collection as one continuous grid.

The preference participates in Gallery settings export/import without changing the export schema version because the existing settings envelope already tolerates additive fields. This continuation also corrects the existing density portability path so exported `viewDensity` is validated and restored on import rather than silently ignored. No MediaStore authority, sort order, mutation, permission, storage, account, or network boundary changes. Pure unit coverage locks grouping defaults and portable identities; representative-device/accessibility acceptance remains open.


## September 29, 2026 — bounded orientation-aware viewer decode record reconciliation

Live PR #100 source already routes authorized full-screen image viewing through `GalleryViewerBitmapLoader`. For image MIME types and a measured viewer viewport, that loader prefers `GalleryViewerImageDecoder`, which uses Android `ImageDecoder` for encoded-orientation handling and bounds decoded output to the current viewport with a 2048px long-edge ceiling. Thumbnail loading remains a fail-safe fallback for decode failure, non-image media posters, or pre-layout states.

Pure `GalleryViewerDecodePolicyTest` coverage verifies image routing, portrait/landscape viewport sizing, no upscaling, the 2048px bound, and fail-closed invalid dimensions. This is useful bounded full-screen image viewing, not an unrestricted original-resolution zoom claim. Representative-device quality/memory/accessibility acceptance and any separately justified full-resolution zoom/pan behavior remain open.


## September 28, 2026 — bounded local photo slideshow

The authorized full-screen Gallery viewer now provides a bounded **Slide / Stop** photo slideshow. It advances every five seconds to the next later image in the current authorized viewer collection, skips non-photo entries, stops at the end, and cancels on Activity pause/destroy, viewer replacement/close, manual Previous/Next, or swipe navigation. It adds no new MediaStore, storage, mutation, account, or network authority.

Pure policy coverage locks forward-only photo progression and fail-closed invalid/end states. Exact candidate head `c30ba640e86f9e4b28f78f9ad7e8b80644eec00f` passed Platform Contract, Native Core, Native Android Adapter, Native Android App, Native Android Rendered Acceptance, and GoreeCloud Gallery acceptance validation. Representative physical-device presentation, large-text top-bar fit, TalkBack/Switch Access, orientation/form-factor behavior, video slideshow behavior, and production/Stable acceptance remain open.


## September 28, 2026 — viewer Fit / Fill presentation control

The authorized full-screen media viewer now exposes an explicit **Fit / Fill** presentation control. **Fit** keeps the complete media visible with `FIT_CENTER`; **Fill** uses `CENTER_CROP` to occupy the viewer canvas. The control carries state-specific accessibility wording and announces mode changes. This is session-local presentation only and does not change MediaStore scope, mutation authority, metadata, favorites, editing, or playback permissions.

Focused policy coverage locks deterministic Fit ↔ Fill toggling. Representative-device image/video behavior, large-text/top-bar fit, TalkBack/Switch Access, orientation changes, and broader viewer acceptance remain open.


**Record type:** Repository implemented-feature inventory  
**Repository:** `GoreeCloud/gallery`  
**Lifecycle:** Development / non-Stable  
**Authority:** Current `main` source and accepted repository evidence  
**Governing standard:** Standard — Repository Feature Tracking and Changelog Governance v1.0, effective September 22, 2026.

## Interpretation

This record describes capabilities present in the current first-party GoreeCloud Gallery Development line. It does **not** establish Production Acceptance, Release Candidate, or Stable qualification. Physical-device, accessibility, performance, recovery, signing, deployment, and complete Integral Platform System acceptance remain separate gates.

`FEATURES.md` remains the product-facing feature description. This file is the lifecycle authority for what is implemented.

## Implemented Development capabilities

### Local media access and browsing

- Android-authorized local image/video access with fail-closed permission handling, including selected-media/partial-access behavior.
- Bounded MediaStore image/video reads through the compiled Android adapter.
- Validated media-item and MediaStore-row domain models.
- Local thumbnail loading with bounded in-memory caching and no cloud dependency.
- Direct Photos, Albums, Videos, and Settings navigation.
- Adaptive Photos and Videos grids grouped by Today, Yesterday, and calendar date.
- Newest/Oldest ordering over the current authorized snapshot.
- Local search over authorized display names and album names.
- Album browsing with covers, names, counts, adaptive layout, and bounded album-detail browsing.

### Favorites, viewer, selection, and sharing

- Device-local Favorites stored only in Gallery application state.
- Full-screen bounded media viewer shell with Previous/Next navigation and contextual actions.
- Read-only Android Share handoff for authorized media content URIs.
- Viewer details for type, album, date, dimensions, duration, and size when available.
- Long-press selection and multi-select with selected-count presentation and contextual actions.
- Bulk Share using `ACTION_SEND` or `ACTION_SEND_MULTIPLE` with bounded read-only URI grants.
- Bulk Favorite/Unfavorite behavior over the current authorized selection.
- Selection Details for a single selected item.
- Framework-independent selection and non-destructive bulk-action policy that resolves only against the current authorized/presented scope.

### Delete, Trash, and Recycle Bin Development boundary

- Android 11+ Delete/Trash candidate using Android-owned confirmation through `MediaStore.createTrashRequest(...)` or `MediaStore.createDeleteRequest(...)`.
- First-party Recycle Bin candidate backed by Android MediaStore Trash.
- Recycle Bin browsing, bounded viewer navigation, Restore, permanent Delete, and multi-select Restore/Purge actions.
- Maximum 100 unique authorized MediaStore image/video item URIs per Trash/Restore/Delete request.
- Rejection of non-MediaStore, file, network, blank, collection-only, malformed, stale, or foreign mutation targets.
- Android 10 and earlier remain fail-closed for this mutation path rather than using an unapproved legacy direct-delete workaround.

### Android-authorized Move Development implementation

- Provider-owned Android MediaStore `RELATIVE_PATH` is represented as bounded metadata in the core model and read through the Android adapter; it is not treated as raw filesystem authority.
- Existing-folder destination policy derives only from authoritative relative paths already visible in the current authorized Gallery scope.
- New-folder destination policy requires one authorized source path, validates bounded folder names, and roots photo/video/mixed destinations in `Pictures/`, `Movies/`, or `DCIM/` respectively.
- Android 11+ move authorization uses `MediaStore.createWriteRequest(...)` over canonical bounded MediaStore item URIs; the adapter independently validates the final destination before updating `MediaStore.MediaColumns.RELATIVE_PATH`.
- Selection mode exposes Move only when the current authorized selection has a safe existing-folder or new-folder destination; new folders use the bounded `Create & move` path.
- Pending move authorization state saves and restores only exact canonical URI lists and an exact canonical destination path across Activity state recreation.
- Android remains write-authorization authority. Cancellation leaves the move unapplied; approved requests execute the bounded MediaStore update off the UI thread, clear selection and thumbnail cache state, refresh Gallery state, and report moved/failed counts.
- This Development implementation is not representative physical-device/OEM/profile Move acceptance and does not establish production-safe media mutation.

### First-party photo editing Development implementation

- Viewer Edit accepts only bounded Android MediaStore `content://media` image sources with supported image MIME types and launches a non-exported Gallery editor Activity.
- The editor provides 90° left/right rotation, horizontal flip, full/custom crop plus centered 1:1, 4:3, and 16:9 crop presets, and reset.
- Rotation, flip, and normalized crop state survive ordinary Activity state recreation.
- Save copy renders the current edit plan and publishes a new MediaStore image while preserving the original source item; PNG remains PNG and other supported output is encoded as JPEG, with source relative path/date-taken metadata retained when Android exposes it.
- Failed save publication is cleaned up rather than knowingly leaving an unpublished partial MediaStore item.
- This bounded editor is a Development foundation; representative-device, accessibility, large-image/memory, metadata, recovery, and broader editing acceptance remain open.

### Settings and portability

- Slow/Fast local thumbnail loading priority.
- Included-folder and excluded-folder presentation controls over already authorized MediaStore scope.
- Show-hidden-items preference without bypassing Android authorization or profile isolation.
- Clear-cache behavior limited to the in-memory thumbnail cache.
- Favorites export/import using versioned local JSON through Android's document provider.
- Settings export/import for non-secret Gallery preferences.
- Rounded-square thumbnail preference.
- Move-deleted-items-to-Recycle-Bin preference controlling Android-confirmed Trash versus permanent delete behavior on Android 11+.
- Persisted future-facing preferences for autoplay, loop, GIF animation, and empty-folder cleanup; these stored preferences do not constitute implementation of the gated behavior they target.

### Presentation and repository controls

- GLAZE UI V1.6 source mapping for the current Development line, without claiming Gallery-specific acceptance.
- Repository validation, security/source checks, Android build/lint/test evidence, and release-engineering documentation retained in the repository.

## Implemented-but-not-accepted boundaries

The following foundations exist but remain acceptance-gated and therefore also appear in `PLANNED-FEATURES.md`:

- Android-authorized Delete/Trash and Recycle Bin behavior pending representative physical-device/OEM/profile validation.
- Android-authorized Move UI/authorization/execution pending representative physical-device/OEM/profile, cancellation/approval, provider-failure, permission-revocation, and post-move acceptance.
- First-party photo editing pending representative-device, accessibility, large-image/memory, save/recovery, and broader editing acceptance.
- Multi-select and bulk actions pending broader device/accessibility acceptance.
- GLAZE UI V1.6 source adoption pending full Gallery rendered/accessibility/adaptive-layout/performance/HVE acceptance.
- Release-engineering foundations without completed production signing, recovery, Release Candidate, or Stable qualification.

## Maintenance rule

When an obligation in `PLANNED-FEATURES.md` becomes implemented and is verified on the authoritative integration line, reconcile it here and record the material change in `CHANGELOGS.md`. Draft or unmerged pull requests are not implementation authority.