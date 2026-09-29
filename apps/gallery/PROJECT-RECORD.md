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

## Legacy Drive roadmap migration ledger — preserved from Gallery PR #92

Before the retired Gallery Drive roadmap could be discarded, legacy identifiers FR-001 through FR-009 were explicitly reconciled. This ledger is retained as historical migration provenance only; it does not restore Drive roadmap authority or duplicate Tasks Management.

- **FR-001 — Roadmap reconciliation control.** Migrated into repository-native interpretation and maintenance rules; the former repository/Drive synchronization requirement is superseded.
- **FR-002 — Tasks Management routing control.** Preserved by the requirement that actionable execution work remain in GoreeCloud Tasks Management when applicable.
- **FR-003 — Evidence/lifecycle control.** Preserved as the requirement for authoritative implementation and verification before lifecycle promotion; the former Drive synchronization clause is retired.
- **FR-004 — Glaze UI source migration.** Historical V1.3 Development work remains provenance, including legacy PR #74 exact head `85472206cc5aab479c3582c144574473ad4ae26a`, PR #75 exact head `e0cdb4edca25824d34ceac71dbc4bd954fffd96a`, and PR #76 exact head `6486b91cb8e29e75f3a483f521f69235542780ba`. The V1.3 target is superseded by later Gallery Glaze authority.
- **FR-005 — Rendered/application acceptance.** Remains represented by current rendered, accessibility, adaptive-layout, performance, rollback, Human Visual Excellence, and representative-device acceptance obligations.
- **FR-006 — First-party photo-editor acceptance.** Remains represented by crop/rotate/flip/reset/save-copy, recreation, output/orientation fidelity, metadata/color behavior, failure/cancellation, accessibility, and representative-device evidence obligations. Historical PR #75 source remains Development provenance only.
- **FR-007 — Recycle Bin and destructive-operation acceptance.** Remains represented by physical-device/OEM/profile, permission, mixed-media, provider-failure, process-recreation, retention/expiry, and recovery obligations.
- **FR-008 — Platform-system integration.** Earlier six-system wording was superseded by evaluation/acceptance of all applicable Integral Platform Systems: Manager, Privacy Shield, Wardveil Security, Everkeep, GLAZE UI, Mesh, Identity, Policy, and Observability; GoreeCloud Sync remains separately governed.
- **FR-009 — Signing, packaging, release, and Stable gates.** Remains represented by signed release packaging, upgrade/recovery, rollback, Release Candidate, production acceptance, and Stable qualification obligations.

Source provenance: legacy `GoreeCloud/gallery` PR #92, head `b85ace4251b47957e64bb1f836ade32eb47365bb`. Current lifecycle truth remains the monorepo feature records and verified repository evidence.

