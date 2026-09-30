# Keyboard migration origin

GoreeCloud Keyboard is mandatorily migrated into `GoreeCloud/android-app-defaults`.

- Source repository: `GoreeCloud/keyboard`
- Imported Development revision: `041b5e0457f2b4d68dc96a416d5abc1f020646d7`
- Legacy cutover pull request: `GoreeCloud/keyboard#97` (closed as superseded; branch retained temporarily pending reconciliation and repository deletion)
- Destination: `apps/keyboard/`
- Import exclusions: repository-scoped `.github/` metadata and generated `artifacts/` output.
- Source validation at cutover: Android CI and Platform Contract checks passed on the imported exact head.
- Lifecycle boundary: this is a Development source migration. It does not establish Release Candidate, Stable, production, representative-device, or other unverified acceptance.
- Retirement boundary: the standalone repository is a temporary migration source only. After all unique required branch/workflow/reference material is reconciled, the standalone repository must be deleted and deletion verified in GitHub.

Repository-scoped legacy workflows are not nested here because GitHub would not execute them from an application subdirectory. Destination-root CI validates the migrated application from this path.
