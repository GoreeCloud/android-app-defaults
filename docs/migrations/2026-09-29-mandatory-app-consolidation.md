# Mandatory Android app consolidation — 2026-09-29

GoreeCloud Camera, GoreeCloud Launcher, GoreeCloud Keyboard, and GoreeCloud Gallery are required to use `GoreeCloud/android-app-defaults` as their source-development repository.

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
- `apps/keyboard/`
- `apps/gallery/`

The repository root Gradle build continues to own the applications already integrated into that build. The migrated-app CI runs the four imported Gradle roots from their new monorepo paths. Shared-build convergence is a separate, reviewable engineering change.

## Validation boundary

Keyboard and Gallery had successful relevant exact-head CI at cutover. Camera and Launcher exact-head source workflows were still running when their legacy PR heads were frozen. Destination monorepo CI therefore validates the relocated paths directly before this migration is eligible to merge.

No source-CI or migration-CI result changes product lifecycle status. These applications remain Development unless their own release criteria establish a later state.

## Legacy repository retirement and deletion

The four standalone repositories are temporary migration sources, not permanent archives. New product source development belongs only in this monorepo after the migration cutover.

Required completion state:

1. Reconcile every remaining legacy branch, pull-request change, issue dependency, release reference, external link, and repository-native dependency that still carries unique required information or source.
2. Preserve required substantive project history in the monorepo project records and migration provenance before destructive retirement.
3. Verify the monorepo default branch contains the accepted migrated state and the four application paths remain buildable under destination CI.
4. Remove or update stale references that would point users or automation at the standalone repositories.
5. Delete `GoreeCloud/camera`, `GoreeCloud/launcher`, `GoreeCloud/keyboard`, and `GoreeCloud/gallery` after the preceding reconciliation is complete.
6. Verify GitHub no longer reports those standalone repositories, then update the canonical GoreeCloud repository inventory from that live state.

Repository deletion is a migration completion requirement. Until deletion is verified, the consolidation remains operationally incomplete even though active source ownership has already moved to this monorepo.
