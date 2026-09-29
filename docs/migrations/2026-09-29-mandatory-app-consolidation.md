# Mandatory Android app consolidation — 2026-09-29

GoreeCloud Camera, GoreeCloud Launcher, GoreeCloud Keyboard, and GoreeCloud Gallery are required to use `GoreeCloud/android-app-defaults` as their source-development repository.

## Imported Development revisions

| Application | Legacy repository | Exact imported revision | Active source PR at migration start |
| --- | --- | --- | --- |
| Camera | `GoreeCloud/camera` | `9b02ca8c19d56868ed30b3d457631e582ef529cb` | #19 |
| Launcher | `GoreeCloud/launcher` | `1eb8dd6d8178f6d730b100559e8f8d149501d18c` | #248 |
| Keyboard | `GoreeCloud/keyboard` | `041b5e0457f2b4d68dc96a416d5abc1f020646d7` | #97 |
| Gallery | `GoreeCloud/gallery` | `3f7263c6e31a0af0f1a06b99491f608b96cc69ba` | #103 |

The migration intentionally preserves the newest active Development integration heads rather than copying only legacy default-branch state. Repository-scoped `.github/` content and generated `artifacts/` are excluded from nested application imports; destination-root workflows own CI and source-integrity validation.

## Validation boundary

Keyboard and Gallery had successful relevant exact-head CI at migration start. Camera's source validation job was successful while runtime-emulator jobs were still running. Launcher's validate and transition-performance jobs were successful, but its Android 16 runtime job failed in `secondaryHomeRendersMovedBuiltInWidget` with a Compose `performMeasureAndLayout called during measure layout` exception. The migration preserves that Development state and does not convert it into an acceptance claim.

## Legacy repositories

The four legacy repositories must remain available as Git-history and migration provenance until open pull requests, issues, release references, external links, and any other repository-native dependencies have been reconciled. New product source development belongs in this monorepo after the migration cutover is merged.
