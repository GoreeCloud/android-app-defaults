# GoreeCloud Gallery Architecture

## Purpose

This document describes the source, runtime, privacy, UI, user-isolation, build, and release architecture of GoreeCloud Gallery.

The current acceptance line is `1.0.0-gc.7`. The repository is the authoritative GoreeCloud development record, while the installable application is reconstructed from pinned upstream Fossify source plus the preserved GoreeCloud modification chain.

## Architectural goals

GoreeCloud Gallery is designed to remain:

- offline-first;
- local-media focused;
- privacy-conscious;
- independently installable as `com.goreecloud.gallery`;
- open source;
- visually governed by Glaze UI;
- reproducible from recorded source revisions;
- safe for personal media when operated within Android permission and storage boundaries;
- maintainable without depending on a GoreeCloud backend service.

## Runtime architecture

```text
Android device
│
├── Android user / profile boundary
│   └── GoreeCloud Gallery app sandbox
│       ├── app preferences and app-private state
│       ├── Android MediaStore / media APIs
│       ├── Storage Access Framework / approved storage paths
│       └── Android intents for approved local/share/open flows
│
└── Local photos, videos, and media
```

There is no GoreeCloud Gallery application server, hosted user database, cloud account, analytics service, advertising service, telemetry service, or remote application API in the intended product architecture.

The packaged APK must not request `android.permission.INTERNET`.

## Multi-user and privacy boundary

GoreeCloud Gallery is a local Android client rather than a shared multi-user server application. It therefore does not create in-app GoreeCloud accounts merely to simulate a server identity model.

Its user separation relies on Android's operating-system user/profile model, application sandbox, permission framework, and media/storage authorization. Each Android user or managed profile receives the isolation provided by the platform. GoreeCloud Gallery must not deliberately cross those boundaries, merge private media between Android users/profiles, or bypass platform authorization.

When practical, stable-release acceptance should verify behavior on a device with a secondary user or managed/work profile. This is the application-appropriate implementation of GoreeCloud's multi-user and private-data-boundary requirement.

## Source architecture

The repository intentionally separates GoreeCloud maintenance logic from the reconstructed upstream working tree.

```text
goreecloud-gallery/
├── .github/             repository governance and CI
├── docs/                architecture, build, signing, and release records
├── patches/             preserved GoreeCloud gc.1-gc.7 source transformations
├── scripts/             reconstruction and validation entry points
├── README.md
├── SECURITY.md
├── CONTRIBUTING.md
└── NOTICE.md

Generated during build only:
├── .build/              materialized patch programs
├── upstream-gallery/    pinned Fossify Gallery working tree
├── upstream-commons/    pinned Fossify Commons working tree
└── dist/                acceptance/release evidence
```

Generated working trees and artifacts are not authoritative source and are excluded from Git.

## Upstream source boundary

The current gc.7 line is pinned to:

- Fossify Gallery 1.13.1 — `b28299dc33821eee8d108a9880ce87876cf31443`
- Fossify Commons 6.1.5 — `acfd352df1a1852d17a5f77def8b7ad6e522a5b6`

`scripts/reconstruct-source.sh` clones and checks out those exact revisions, applies the GoreeCloud patch chain in order, and refuses to reuse pre-existing source directories.

Accepted historical patch programs are provenance. Future product changes should normally extend the ordered patch line rather than rewriting an already accepted historical transformation.

## Glaze UI architecture

Glaze UI is a release requirement for GoreeCloud-controlled user-facing surfaces. For Gallery, the implementation emphasizes:

- rounded media, cards, containers, dialogs, and controls;
- layered light and dark surfaces;
- restrained depth rather than heavy visual ornament;
- readable popup and dialog contrast;
- touch-friendly interaction sizing;
- consistent GoreeCloud product identity;
- accessibility and content readability ahead of decorative effects;
- predictable behavior across the folder browser, opened media folders, settings, dialogs, and toolbar surfaces.

Glaze UI does not require translucency everywhere. Performance, contrast, accessibility, and reliable media interaction take priority over decorative glass effects.

Source invariants protect several accepted Glaze behaviors, including rounded thumbnails and readable light/dark toolbar popup themes.

## File-operation security boundary

Gallery can perform operations that affect user-owned media. Copy, move, rename, edit, delete, trash/recycle-bin, restore, and permanent-delete behavior must therefore be treated as data-protection-sensitive.

Stable acceptance must use disposable copied media and should verify the actual filesystem/media result in addition to the visible UI result where practical.

The application must not treat its own app-private state as the only backup or recovery path for user media.

## Build architecture

The ordinary acceptance path is:

```text
Repository commit
   ↓
Repository structure/security guardrails
   ↓
Exact upstream source reconstruction
   ↓
GoreeCloud source invariants
   ↓
Android test task + lint
   ↓
FOSS debug APK
   ↓
Packaged APK validation
   ↓
Artifact + checksum + license/notice + validation evidence
```

The normal workflow uses read-only GitHub repository permissions, non-persisted checkout credentials, explicit Ubuntu 24.04, reviewed full-SHA third-party Actions, and exact revision checkout.

## Release-signing architecture

Stable signing is deliberately separate from normal pull-request CI.

The manual signed release-candidate workflow:

1. runs only through `workflow_dispatch`;
2. uses the `stable-release` GitHub environment;
3. reconstructs the same pinned source;
4. runs source and build validation;
5. obtains signing material only from protected environment secrets;
6. aligns and signs the release APK;
7. verifies the final signature and approved certificate SHA-256 fingerprint;
8. uploads a signed candidate and evidence;
9. does not publish a stable GitHub Release automatically.

Long-lived signing material remains outside source control and ordinary documentation.

## Validation architecture

Validation is layered so one check does not stand in for another:

- repository structure validation protects expected project records and layout;
- repository security validation protects CI and secret-handling boundaries;
- source reconstruction protects exact provenance;
- source invariants protect GoreeCloud-specific source behavior;
- Android lint and test tasks protect build quality;
- APK validation protects the final package identity, network boundary, signature, notice, removed-warning boundary, and checksum;
- real-device acceptance protects permission-sensitive, destructive, visual, accessibility, and upgrade/recovery behavior.

A task that reports `NO-SOURCE` is not counted as meaningful behavioral test coverage.

## Release classification

The repository distinguishes among:

- development work;
- acceptance candidates;
- signed release candidates;
- stable releases.

Passing CI advances evidence quality but does not automatically advance release classification. `docs/STABLE-RELEASE-CHECKLIST.md` is the blocking stable-promotion contract.

## Recovery and maintainability

Source recovery depends on:

- this GoreeCloud repository and its Git history;
- the exact upstream source revisions;
- the preserved ordered patch chain;
- documented build tooling;
- a recoverable long-lived stable signing identity after first stable publication.

User media recovery is separate from source recovery. Personal photos and videos must remain independently protected outside the application package and source repository.