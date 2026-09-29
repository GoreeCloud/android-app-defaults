# Gallery migration origin

GoreeCloud Gallery is mandatorily migrated into `GoreeCloud/android-app-defaults`.

- Source repository: `GoreeCloud/gallery`
- Imported Development revision: `3f7263c6e31a0af0f1a06b99491f608b96cc69ba`
- Legacy cutover pull request: `GoreeCloud/gallery#103` (closed as superseded; branch retained)
- Destination: `apps/gallery/`
- Import exclusions: repository-scoped `.github/` metadata and generated `artifacts/` output.
- Source validation at cutover: Native Android App, Native Android Adapter, and Native Android Rendered Acceptance checks passed on the imported exact head.
- Lifecycle boundary: this is a Development source migration. It does not establish Release Candidate, Stable, production, representative-device, or other unverified acceptance.
- History boundary: the standalone repository remains historical Git provenance until repository-retirement reconciliation is complete.

Repository-scoped legacy workflows are not nested here because GitHub would not execute them from an application subdirectory. Destination-root CI validates the migrated application from this path.
