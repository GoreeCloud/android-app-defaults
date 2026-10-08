# Mandatory Android app consolidation — 2026-09-29

At its September 29, 2026 cutover, this monorepo imported Camera, Launcher, Keyboard, and Gallery. The October 8, 2026 owner directive supersedes that requirement for Keyboard: `apps/keyboard/` is retired, and new Keyboard development belongs in the separate `GoreeCloud/keyboard` repository. This document retains historical cutover evidence while describing the current ownership exception.

## Imported Development revisions

| Application | Legacy repository | Exact imported revision | Legacy cutover PR |
| --- | --- | --- | --- |
| Camera | `GoreeCloud/camera` | `209b89ca31cc23899f6c90d7ca12908281f0af0c` | #19 |
| Launcher | `GoreeCloud/launcher` | `1eb8dd6d8178f6d730b100559e8f8d149501d18c` | #248 |
| Keyboard | `GoreeCloud/keyboard` | `041b5e0457f2b4d68dc96a416d5abc1f020646d7` | #97 |
| Gallery | `GoreeCloud/gallery` | `3f7263c6e31a0af0f1a06b99491f608b96cc69ba` | #103 |

The migration preserves the newest active Development integration heads at cutover rather than copying only legacy default-branch state. The four active legacy integration PRs were then closed as superseded. Legacy branches remain temporarily available only while remaining unique work and references are reconciled.

Repository-scoped `.github/` content and generated `artifacts/` are excluded from the nested application imports. Destination-root workflows own CI and source-integrity validation.

## Build topology

Each migrated application initially retains its own Gradle root inside its destination directory. This avoids silently rewriting build systems while relocating source ownership:

- `apps/camera/`
- `apps/launcher/`
- `apps/gallery/`

The repository root Gradle build continues to own the applications already integrated into that build. The migrated-app CI runs the three retained imported Gradle roots from their new monorepo paths. Shared-build convergence is a separate, reviewable engineering change.

## Validation boundary

Keyboard and Gallery had successful relevant exact-head CI at cutover. Camera and Launcher exact-head source workflows were still running when their legacy PR heads were frozen. Destination monorepo CI therefore validates the relocated paths directly before this migration is eligible to merge.

No source-CI or migration-CI result changes product lifecycle status. These applications remain Development unless their own release criteria establish a later state.

## Post-cutover reconciliation

The mandatory cutover intentionally preserved only the active Development integration heads. Before deleting the standalone repositories, the remaining divergent legacy branches were reviewed through target issues #63, #65, #66, and #67.

Material reconciliation carried by this finalization candidate includes:

- **Camera:** all non-documentation legacy branches are ancestry-contained in the imported cutover. The remaining project-specification/project-record branch is incorporated into `apps/camera/PROJECT-SPECIFICATIONS.md` and `PROJECT-RECORD.md`.
- **Gallery:** the unique FR-001…FR-009 legacy roadmap disposition ledger from standalone PR #92 is preserved in `apps/gallery/PROJECT-RECORD.md`. Older search, selection, Glaze, Move, and physical-device stabilization branches were reviewed as superseded by newer current source or explicitly retired product direction. The project-governance branch is incorporated into the canonical project records.
- **Keyboard (historical only):** source, tests, assets, project records, and historical patch files previously imported under `apps/keyboard/` are retired from this monorepo following the October 8 owner directive. The new independent repository does not inherit monorepo Development or validation status.
- **Launcher:** the reusable Platform Contract 0.4 validator pin from standalone PRs #218/#251 is translated into monorepo-root `.github/workflows/migrated-platform-contract.yml`. Legacy PR #79 exposed a lost Room dependency delta, so the Launcher Room plugin/runtime/compiler are restored from 3.0.1 to 3.0.2 for current exact-head validation. Earlier Unicode search/sort, transactional restore, security/privacy, icon/inventory, Home/page, and Glaze work was verified as present or superseded; obsolete group-operation prototypes remain planned/open rather than being misrepresented as accepted source.
- **Repository-scoped CI:** because nested `.github/workflows` do not execute as application workflows, relevant Platform Contract validation is reproduced at monorepo root instead of copying legacy workflow directories into `apps/*`.
- **Migration integrity:** the one-time frozen-tree equality check is replaced with permanent provenance enforcement so legitimate post-cutover development no longer fails merely because app source changes after migration.

The earlier retirement issue #72 does not authorize deletion of the independent Keyboard repository. Target issue #72 controlled historical destructive retirement. Repository deletion is permitted only after the reconciliation issues are complete, the accepted monorepo default branch is read back, destination CI is green, required references/dependencies are updated, and the canonical repository index can be reconciled from verified post-deletion GitHub state.

## Legacy repository retirement and deletion

For Camera, Launcher, and Gallery, these were historical legacy-repository retirement requirements. **`GoreeCloud/keyboard` is exempt** because it is the new independent home for Keyboard development; do not delete it under the former consolidation plan.

Required completion state:

1. Reconcile every remaining legacy branch, pull-request change, issue dependency, release reference, external link, and repository-native dependency that still carries unique required information or source.
2. Preserve required substantive project history in the monorepo project records and migration provenance before destructive retirement.
3. Verify the monorepo default branch contains the accepted migrated state and the three retained imported application paths remain buildable under destination CI.
4. Remove or update stale references that would point users or automation at the standalone repositories.
5. Retire `GoreeCloud/camera`, `GoreeCloud/launcher`, and `GoreeCloud/gallery` when their own prerequisites are met; retain the independent `GoreeCloud/keyboard` repository.
6. Verify live GitHub state for the remaining legacy repositories and the retained independent Keyboard repository; reconcile the canonical repository inventory from that verified state.

Legacy repository retirement applies to the remaining consolidated applications only. The independent Keyboard repository is not subject to this migration-deletion requirement.
