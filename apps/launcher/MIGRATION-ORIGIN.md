# Launcher migration origin

This application is mandatorily migrated into  by the GoreeCloud owner directive dated 2026-09-29.

- Source repository: 
- Imported Development revision: 
- Source pull request at migration start: #248
- Destination: 
- Import exclusions: repository-scoped  metadata and generated  output.
- Source validation at migration start: Android CI was in progress at the refreshed imported head at migration cutover; the immediately preceding head had a failing Android 16 Room/runtime test and was therefore not used as the final cutover revision.
- Lifecycle boundary: this is a Development source migration. It does not establish Release Candidate, Stable, production, representative-device, or other unverified acceptance.
- History boundary: the standalone source repository remains the historical Git provenance until repository-retirement reconciliation is complete.

Repository-scoped workflows are not copied into a nested application directory because GitHub would not execute them there. The destination repository owns monorepo-level validation and migration-integrity checks.
