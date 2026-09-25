# Android App Defaults — Project Record

**Repository:** `GoreeCloud/android-app-defaults`  
**Record purpose:** Significant project history, governance decisions, migrations, and evidence  
**Lifecycle:** Development  
**Canonical authority:** This file is the repository-local project record once accepted on the default branch.

## 2026-09-21 — Repository and Drive specification established

- GitHub reports the repository was created on September 21, 2026.
- The former Google Drive **Project Specification — Android App Defaults** was also created September 21, 2026 and defined the initial privacy-focused Android utility monorepo direction.
- The Drive-era document treated the repository as primarily planned scope and explicitly warned that planned capabilities were not evidence of implementation.

## 2026-09-23 — GoreeCloud Since becomes the first implemented application

- GoreeCloud Since became the first application module under `apps/since/`.
- PR #2 established the Android Development foundation and was squash-merged as `8afd4eecc1a374443f7a7b7da72eb19e79dfc4e1` after exact-candidate CI.
- Subsequent Development PRs added Room persistence, persistent create/Dashboard/Details, custom-start/Edit behavior, accessibility evidence, localization/RTL evidence, UI refinement, goal progress/editor behavior, and related verification.
- Detailed feature and change evidence remains in `IMPLEMENTED-FEATURES.md` and `CHANGELOGS.md`; this project record intentionally avoids duplicating routine changelog detail.

## 2026-09-23 — Drive specification version 1.1.0

The Drive project specification was updated to version 1.1.0 and continued to define a broader planned suite. It included Clock, Calculator, Calendar, Contacts, Notes, Voice Recorder, Compass, Flashlight, Gallery, File Manager, Since, and possible future utilities.

## 2026-09-24 — Repository scope reconciliation

Live GitHub verification established that Calendar, Contacts, Notes, Gallery, and File Manager are independently maintained repositories:
- `GoreeCloud/goreecloud-calendar`
- `GoreeCloud/contacts`
- `GoreeCloud/goreecloud-notes`
- `GoreeCloud/gallery`
- `GoreeCloud/file-manager`

This supersedes the Drive-era assumption that those projects should be owned by `android-app-defaults`. Their project-specific requirements must be reconciled into their own repositories before the shared Drive source is eligible for deletion.

At the same verification point:
- `settings.gradle.kts` includes only `:apps:since`;
- `apps/since/` is the only application directory under `apps/`;
- repository lifecycle remains Development;
- the repository has no active branch protection and no repository rulesets;
- the root contains `README.md`, `IMPLEMENTED-FEATURES.md`, `PLANNED-FEATURES.md`, and `CHANGELOGS.md`, but did not yet contain the mandatory project specification/record files.

## 2026-09-24 — Project-governance migration candidate

Migration pull request: [PR #45](https://github.com/GoreeCloud/android-app-defaults/pull/45).

This branch introduces:
- `PROJECT-SPECIFICATIONS.md` as the canonical repository-local normative project specification;
- `PROJECT-RECORD.md` as the significant historical/governance record;
- README navigation to both records.

Migration source:
- Google Drive: **Project Specification — Android App Defaults**, file ID `1SsLEnAy24H1dF-PP0XEDwz8QYNO5WFwmcKir-3qh04Y`.
- Source version: 1.1.0.
- Source last updated: September 23, 2026.

Deletion status: **Blocked / not eligible.** The Drive source covers multiple projects and must remain until the independent-project portions are migrated and the repository migration is accepted and read back from the authoritative branch.

## Evidence and related records

- [README.md](README.md)
- [PROJECT-SPECIFICATIONS.md](PROJECT-SPECIFICATIONS.md)
- [IMPLEMENTED-FEATURES.md](IMPLEMENTED-FEATURES.md)
- [PLANNED-FEATURES.md](PLANNED-FEATURES.md)
- [CHANGELOGS.md](CHANGELOGS.md)

## Record maintenance rule

Update this file for significant architecture, ownership, governance, repository migration, lifecycle, security/privacy redesign, major production/recovery events, repository split/merge/rename, or retirement events. Routine feature/fix detail belongs primarily in `CHANGELOGS.md`.
