# GoreeCloud Gallery native replacement boundary

## Direction

GoreeCloud Gallery is being replaced by an original GoreeCloud-owned Android application built natively from the ground up. The existing Fossify-based reconstruction and gc patch history remain useful as bounded transitional material for behavior comparison, migration planning, licensing/provenance obligations, regression reference, and continuity while the native application becomes complete.

The inherited application is not the long-term GoreeCloud Gallery architecture. New Gallery product behavior should advance the first-party native implementation unless a narrowly documented migration or compatibility need requires a change to the transitional line.

## Current first-party native source

The `native/core` module establishes framework-independent local-media domain behavior owned by GoreeCloud. Current source includes:

- validated image/video media items;
- deterministic media filtering and sorting;
- local media mutation contracts;
- trash/recovery domain behavior;
- authoritative album metadata and deterministic album summaries; and
- focused native-core tests.

The repository also contains an early Android-facing media repository boundary under `native/src`. These foundations do not yet constitute a complete installable native Gallery application.

## Album boundary

The native album catalog accepts album identity only when the platform adapter supplies both a nonblank album identifier and a nonblank display name. The core does not invent a fake folder or album for media that lacks authoritative grouping metadata. If one album identifier arrives with conflicting display names in the same catalog snapshot, the model fails closed rather than silently merging inconsistent state.

This is intended to map cleanly to Android-authorized local media metadata such as MediaStore bucket information without making the domain dependent on a specific Android API.

## Transitional Fossify line

The preserved Fossify 1.13.1 / Commons 6.1.5 reconstruction and gc patch chain remain historical and transitional evidence. Existing acceptance artifacts from that line must continue to be described according to what they actually validate; they must not be presented as proof that the first-party native replacement is complete, released, or Stable.

Any continued maintenance of the transitional line must preserve applicable GPLv3 obligations and exact upstream provenance.

## Stable boundary

GoreeCloud Gallery cannot qualify as Stable until the original native application itself has the required Android media access, browsing, album, viewer, editing/sharing as approved, destructive-operation authorization, profile isolation, accessibility, Glaze UI, Privacy Shield, Wardveil Security, Everkeep, packaging/signing, upgrade/recovery, and representative real-device acceptance evidence applicable to its final scope.
