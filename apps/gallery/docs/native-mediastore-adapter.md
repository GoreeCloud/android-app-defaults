# Native MediaStore Adapter Boundary

## Purpose

This milestone implements the first compiled first-party Android bridge between local `ContentResolver`/MediaStore rows and the authoritative GoreeCloud Gallery native media model.

The provider-neutral normalization contract remains in `native/core`. A separate `native/android-adapter` Android library now compiles against the Android framework, depends on the native core, and owns local MediaStore query/cursor interpretation. It does not yet provide a complete Gallery application shell or UI.

## Projection

`MediaStoreProjection.columns` defines the minimum row surface and is now verified against the Android framework constants used by `AndroidMediaStoreProjection`:

- `_id`
- `_display_name`
- `mime_type`
- `datetaken`
- `date_modified`
- `width`
- `height`
- `duration`
- `_size`
- `bucket_id`
- `bucket_display_name`

The `datetaken` literal corrects the earlier conceptual `date_taken` spelling and matches Android's `MediaStore.MediaColumns.DATE_TAKEN` contract. Unit validation keeps the framework-derived adapter list and pure-Kotlin core projection identical so future column drift fails before device runtime.

## Android query boundary

`AndroidMediaStoreReader`:

- queries `MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)`;
- selects only MediaStore rows whose framework media type is image or video;
- orders newest capture time first, then modification time and provider ID;
- requires a caller-controlled bound between 1 and 500 provider rows, with a default of 250;
- obtains every expected column index before consuming provider rows;
- treats a null provider cursor as `MediaStoreQueryUnavailableException` instead of an authoritative empty library;
- converts provider rows into `MediaStoreRow`, then delegates validation/normalization to the native core; and
- counts structurally invalid individual rows as rejected instead of fabricating Gallery items from them.

The adapter does not perform network, browser, cloud-sync, or third-party metadata lookup. Local Android MediaStore remains the authority for this source.

## Normalization

`MediaStoreRow.toMediaItem()`:

- requires a `content://` collection URI;
- requires a non-negative provider ID;
- accepts only image or video MIME types, case-insensitively;
- converts Android `DATE_TAKEN` milliseconds to `Instant`;
- converts Android `DATE_MODIFIED` seconds to `Instant`;
- derives the item content URI from the queried collection URI and provider ID;
- keeps image duration unset even when a provider row contains a duration value;
- maps album metadata only when both bucket ID and bucket display name are complete and nonblank;
- leaves incomplete bucket metadata ungrouped instead of fabricating an album; and
- fails closed for invalid dimensions, negative sizes/timestamps, unsupported media, or invalid URI authority.

## Build validation

The existing `Native Core` workflow continues validating the standalone JVM model. A new `Native Android Adapter` workflow builds the native multi-project tree, reruns core tests, runs Android-adapter unit tests, and assembles the adapter's debug AAR path. This proves the Android framework bridge compiles; it is not device acceptance.

## Current Acceptance Boundary

This milestone does not establish:

- a native Android Gallery application/activity shell;
- runtime media permission UX/policy or partial-photo-access handling;
- device/emulator MediaStore query acceptance;
- rendered timeline, album, or viewer UI;
- MediaStore write/delete/move authorization;
- cloud media-provider integration;
- Privacy Shield, Wardveil Security, Everkeep, or Glaze UI production integration acceptance;
- release signing, deployment, or Stable qualification.

Those require separate implementation and evidence.
