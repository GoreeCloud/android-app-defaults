# Contributing to GoreeCloud Gallery

GoreeCloud Gallery is a GoreeCloud-maintained Android gallery application based on pinned Fossify Gallery and Fossify Commons source. Contributions must preserve the offline, privacy-first product boundary, the deterministic source-provenance model, Glaze UI, upstream licensing, and the stable-release safety gates.

## Before changing source behavior

Read:

- `README.md`;
- `docs/ARCHITECTURE.md`;
- `docs/BUILD-AND-RELEASE.md`;
- `docs/STABLE-RELEASE-CHECKLIST.md`;
- `SECURITY.md`.

The current accepted `gc.1` through `gc.7` patch history is provenance. Do not rewrite an accepted historical patch merely to make a new change easier. A new application behavior or UI change should normally be introduced as a new ordered GoreeCloud patch line so the accepted history remains reconstructable and reviewable.

## Development principles

Changes must remain consistent with these project requirements:

- offline-first operation and no unnecessary network capability;
- no analytics, advertising, tracking, cloud account requirement, or remote API dependency;
- no reusable secret, private key, keystore, password, token, or personal media in Git;
- Glaze UI for every GoreeCloud-controlled user-facing surface;
- Android accessibility and practical touch targets;
- safe file and media operations;
- Android user/profile and application-sandbox isolation;
- preserved GNU GPL v3 obligations and upstream attribution;
- reproducible builds from the recorded upstream revisions;
- fail-closed validation when assumptions no longer hold.

## Branch and pull-request workflow

Use a focused branch and pull request for material changes. Keep unrelated cleanup out of the same change unless it is necessary to leave the affected area correct and maintainable.

Before requesting merge:

1. run repository guardrails;
2. reconstruct the exact source;
3. run source invariants;
4. run the applicable Android test and lint tasks;
5. build the acceptance APK;
6. validate the packaged APK;
7. retain relevant test/lint/build evidence;
8. describe any manual real-device acceptance that the change requires.

A green CI run does not waive manual acceptance requirements for permission-sensitive, destructive, accessibility, visual, signing, or upgrade/recovery behavior.

## Local validation

From the repository root:

```bash
bash scripts/validate-repository-structure.sh
bash scripts/validate-repository-security.sh
bash scripts/reconstruct-source.sh
bash scripts/validate-source-invariants.sh
```

Then, from `upstream-gallery/`, run the applicable Gradle tasks. The ordinary acceptance workflow currently uses:

```bash
./gradlew --no-daemon --stacktrace :app:testFossDebugUnitTest :app:lintFossDebug
./gradlew --no-daemon --stacktrace :app:assembleFossDebug
```

The upstream unit-test task may report `NO-SOURCE`. That means the task did not fail; it does not prove meaningful behavioral test coverage. GoreeCloud-owned tests should be added when behavior can be tested reliably without creating fragile maintenance coupling.

## Glaze UI changes

Glaze UI changes should preserve a recognizable GoreeCloud experience while remaining readable, accessible, performant, and appropriate for a local photo application. Use rounded geometry, layered surfaces, restrained depth, readable light/dark contrast, consistent controls, and deliberate interaction feedback. Decorative glass effects are not a reason to reduce contrast or usability.

When changing a visual surface, review both light and dark appearance and include real-device evidence when the behavior cannot be represented reliably in automated tests.

## File-operation changes

Any change that can copy, move, rename, edit, delete, trash, restore, or permanently delete media is security- and data-protection-sensitive. Test with disposable copied media only. Verify both the UI result and the underlying file/media result where practical.

## Signing and releases

Do not add release keystores or signing secrets to source control. Normal pull-request CI produces an acceptance build. Stable signing is performed only through the protected manual signed-release-candidate path described in `docs/RELEASE-SIGNING.md`.

Do not describe a build as stable merely because it compiles, installs, or passes CI. Stable promotion requires all blocking items in `docs/STABLE-RELEASE-CHECKLIST.md` and a deliberate publication decision.

## Licensing and provenance

GoreeCloud Gallery remains a modified GPLv3-covered work. Preserve upstream notices, exact upstream revisions, GoreeCloud modification notices, and the ability to reconstruct the corresponding modified source.

Do not remove attribution or license material as part of rebranding.

## Security reports

Do not place exploit details or sensitive data in a public pull request. Follow `SECURITY.md` for vulnerability reporting.