# Native Gallery Selection and Multi-Select

Status: Development foundation; rendered long-press selection UI not yet connected.

## Purpose

The historical GoreeCloud Gallery product model includes selection and multi-select with contextual actions. The native replacement must restore that capability without allowing selection state to create or expand media authority.

## Current implemented core

`GallerySelectionPolicy` operates only over a caller-supplied current authorized/presented `MediaItem` scope. It provides bounded toggle, select-all, prune, and resolve operations. A content URI that is not present in the supplied current scope cannot become selected, and selections that become stale when the scope changes are pruned.

`GalleryBulkActionPolicy` plans the first non-destructive bulk actions over that bounded selection:

- Share preserves current presentation order and derives the narrowest safe MIME type: an exact common MIME type where possible, `image/*` or `video/*` for mixed subtypes of one media family, and `*/*` only for a mixed image/video selection.
- Favorites chooses Add unless every selected authorized item is already a Favorite, in which case the planned action is Remove.

Focused JVM tests cover scope rejection, toggle/remove behavior, select-all bounds, stale-selection pruning, presentation-order preservation, Share MIME planning, and Favorites action planning.

## Rendered follow-on

The next Android UI tranche should connect these contracts to:

- long-press on a media tile to enter selection;
- tap-to-toggle while selection is active;
- visible Glaze selection state on selected thumbnails;
- a contextual bottom action surface replacing ordinary navigation while selection is active;
- active bulk Share and Favorite/Unfavorite through existing read-only/app-local authorities;
- explicit exit/cancel selection behavior and accessibility announcements;
- deterministic pruning when permission, destination, search, album, folder-visibility, or authorized-snapshot scope changes.

## Mutation boundary

Selection does not authorize destructive or organizational media writes. Delete/Trash, Restore/Purge, Move, Copy, Edit, album membership changes, and similar actions remain unavailable until their separate Android-authorized mutation paths, confirmations, recovery semantics, and applicable Privacy Shield / Wardveil / Everkeep responsibilities are implemented and accepted.

No network, cloud, cross-profile, broader MediaStore, or hidden-media authority is added by this foundation.
