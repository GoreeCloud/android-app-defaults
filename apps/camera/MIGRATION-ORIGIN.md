# Camera migration origin

GoreeCloud Camera is mandatorily migrated into `GoreeCloud/android-app-defaults`.

- Source repository: `GoreeCloud/camera`
- Imported Development revision: `209b89ca31cc23899f6c90d7ca12908281f0af0c`
- Legacy cutover pull request: `GoreeCloud/camera#19` (closed as superseded; branch retained)
- Destination: `apps/camera/`
- Import exclusions: repository-scoped `.github/` metadata and generated `artifacts/` output.
- Source validation at cutover: exact-head Android Foundation validation was still running when the cutover was frozen.
- Lifecycle boundary: this is a Development source migration. It does not establish Release Candidate, Stable, production, representative-device, or other unverified acceptance.
- History boundary: the standalone repository remains historical Git provenance until repository-retirement reconciliation is complete.

Repository-scoped legacy workflows are not nested here because GitHub would not execute them from an application subdirectory. Destination-root CI validates the migrated application from this path.
