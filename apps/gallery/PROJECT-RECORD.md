# GoreeCloud Gallery — Project Record

## 2026-09-29 — Mandatory repository consolidation

Active source development moved from `GoreeCloud/gallery` to `GoreeCloud/android-app-defaults` at `apps/gallery/`. The imported Development revision was `3f7263c6e31a0af0f1a06b99491f608b96cc69ba`; the consolidation merged through target PR #62 as `20ca33c7c0c565f361deda206c9c950154dec327`. Legacy documentation migration PR #97 is preserved as source provenance for this repository-local record. No lifecycle promotion is implied.

> **Current repository authority — September 29, 2026:** GoreeCloud Gallery is maintained in `GoreeCloud/android-app-defaults` under `apps/gallery/`. The standalone `GoreeCloud/gallery` repository is a temporary legacy source pending final reconciliation and required deletion. The mandatory cutover imported exact Development revision `3f7263c6e31a0af0f1a06b99491f608b96cc69ba` and merged into monorepo `main` as `20ca33c7c0c565f361deda206c9c950154dec327`. This repository-location statement supersedes legacy repository or “current candidate” wording preserved below when that wording predates the cutover. Historical legacy PR/commit references remain valid evidence, but new project documentation maintenance belongs here.


**Repository:** `GoreeCloud/android-app-defaults` (`apps/gallery/`)  
**Former repository identity in the Drive source:** `GoreeCloud/goreecloud-gallery`  
**Lifecycle:** Development / non-Stable  
**Record purpose:** Significant product history, repository/native-transition decisions, migration provenance, candidate boundaries, and project-specification migration evidence  
**Migration baseline:** `1a241ddb23205f15f673b2968b56fd612014ca87`  
**Canonical authority:** This file is the repository-local project record once accepted on the default branch.

## Product lineage

GoreeCloud Gallery is an offline-first local-media Android product. Its historical experience drew interaction and information-architecture inspiration from Samsung Gallery while remaining GoreeCloud-owned. Historical screenshots and behavior are retained as migration/visual-review evidence, not permission to copy proprietary assets or implementation.

## Transitional Fossify period

The repository preserves a Fossify Gallery/Commons reconstruction and GoreeCloud patch line as provenance, regression reference, migration comparison, and historical continuity.

That line produced a historical 1.0.0 acceptance candidate, but its acceptance evidence applies only to the inherited/transitional binary. It cannot establish Stable status for the native replacement.

## Native replacement decision

The long-term architecture is the original first-party implementation under `native/`, with framework-independent domain logic separated from Android MediaStore adapters and the application shell.

Over time, accepted `main` gained native media/album models, MediaStore normalization, a compiled Android adapter, an installable application shell, local browsing, Albums, Favorites, search, viewer foundations, Android-authorized Trash/Delete, Recycle Bin, selection/bulk actions, local settings/portability, and Move foundations.

## Repository rename reconciliation

The Drive source names the repository `GoreeCloud/goreecloud-gallery`. That repository was later renamed/reconciled as `GoreeCloud/gallery`, and the September 29 mandatory consolidation subsequently moved active development authority to `GoreeCloud/android-app-defaults` under `apps/gallery/`.

The monorepo identity and `apps/gallery/` path control new development links, documentation, pull requests, and maintenance. Both standalone Gallery repository names remain temporary legacy provenance pending reconciliation and required repository deletion.

## Drive specification interpretation

The Drive **Project Specification — Gallery.docx** contains nine top-level sections:
1. Purpose and feature-preservation requirements.
2. Native application architecture.
3. Local media and album authority.
4. Privacy, security, and continuity boundaries.
5. Glaze UI boundary.
6. Optional GoreeCloud Photos integration.
7. Transitional Fossify reconstruction.
8. Current Development state and extensive exact-revision candidate history.
9. Stable qualification gates plus accumulated native milestone evidence.

Those sections have been reconciled into `PROJECT-SPECIFICATIONS.md`. Exact historical version pins, PR heads, CI runs, transitional-candidate statements, and older lifecycle snapshots remain source-era evidence and do not override current `main`.

## Current accepted authority

At migration baseline `1a241ddb23205f15f673b2968b56fd612014ca87`, current implementation state is governed by `IMPLEMENTED-FEATURES.md`, open obligations by `PLANNED-FEATURES.md`, and meaningful accepted chronology by `CHANGELOGS.md`.

The current first-party native line remains Development/non-Stable despite substantial accepted capability work.

## Open candidate/history boundary

The repository contains numerous open Draft pull requests and historical stacked branches. Their exact-head CI, rendered, physical-device, or feature evidence remains scoped to those revisions unless the corresponding work has been integrated into authoritative `main`.

Migration documentation must not revive stale candidate state when newer `main` evidence exists.

## 2026-09-24/25 — Project governance migration candidate

This migration:
- creates root `PROJECT-SPECIFICATIONS.md`;
- creates root `PROJECT-RECORD.md`;
- consolidates the former root `SPECIFICATIONS.md` into the mandatory canonical filename;
- reconciles the Drive source's old repository name;
- preserves Drive-era architecture/product/stability requirements without overwriting newer main state;
- updates README navigation; and
- retires the duplicate root `SPECIFICATIONS.md` on the migration branch after incorporation.

**Drive source:** Project Specification — Gallery.docx  
**Drive file ID:** `1kt0iQrPU0lvZsXw9wmRwhVOjoJ2VH0Ai`  
**Drive deletion status:** Blocked until the migration is accepted, authoritative default-branch readback succeeds, review/check gates pass, and no reconciliation discrepancy remains.

## Ongoing maintenance

Update this file for significant architecture, repository rename/split, Android authority changes, platform-system boundary changes, production/recovery events, lifecycle promotions, migration decisions, or retirement. Routine implementation chronology remains in `CHANGELOGS.md`.
