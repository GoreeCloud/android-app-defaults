# GoreeCloud Camera — Project Record

## 2026-09-29 — Mandatory repository consolidation

Active source development moved from `GoreeCloud/camera` to `GoreeCloud/android-app-defaults` at `apps/camera/`. The imported Development revision was `209b89ca31cc23899f6c90d7ca12908281f0af0c`; the consolidation merged through target PR #62 as `20ca33c7c0c565f361deda206c9c950154dec327`. Legacy documentation migration PR #17 is preserved as source provenance for this repository-local record. No lifecycle promotion is implied.


> **Current repository authority — September 29, 2026:** GoreeCloud Camera is maintained in `GoreeCloud/android-app-defaults` under `apps/camera/`. The standalone `GoreeCloud/camera` repository is historical Git provenance only. The mandatory cutover imported exact Development revision `209b89ca31cc23899f6c90d7ca12908281f0af0c` and merged into monorepo `main` as `20ca33c7c0c565f361deda206c9c950154dec327`. This repository-location statement supersedes legacy repository or “current candidate” wording preserved below when that wording predates the cutover. Historical legacy PR/commit references remain valid evidence, but new project-record maintenance belongs here.


**Document Type:** Repository-Native Project Record  
**Status:** Active  
**Project:** GoreeCloud Camera  
**Repository:** GoreeCloud/camera  
**Authority:** Repository-local project record  
**Last Updated:** 2026-09-27

## 2026-09-27 — Project specification migration staged

The project specification and significant project history are being migrated from transitional GoreeCloud Drive sources into PROJECT-SPECIFICATIONS.md and PROJECT-RECORD.md.

The migration uses current GitHub main as authority for accepted implementation state and uses the Drive records for requirements and historical preservation. This prevents the older Drive snapshot from overwriting newer repository evidence.

The migration also reconciles the historical repository name GoreeCloud/android-app-defaults (`apps/camera/`) to the live GoreeCloud/camera repository.

The Drive project-specification sources remain protected until the migration pull request is accepted, the default-branch files are read back successfully, references are reconciled, and no migration discrepancy remains.

## 2026-09-27 — Feature-state migration accepted

Pull request #16, "Migrate feature tracking from Drive," was merged to main as signed commit 6fbe38f76675b36f3f6278c193645fac4816c6d4.

Feature-state authority moved to IMPLEMENTED-FEATURES.md, PLANNED-FEATURES.md, and CHANGELOGS.md.

The richer former Drive feature roadmap was preserved under docs/history as non-authoritative migration evidence.

This did not promote the Camera lifecycle or establish production readiness.

## Current accepted main boundary

Current main remains lifecycle Concept.

Accepted repository state includes the native Android foundation, Camera2 preview, one bounded JPEG + MediaStore still-capture path with representative Android-emulator qualification, and a bounded Camera2/MediaRecorder video-with-audio source/build foundation.

The accepted main history through PR #9 does not establish physical-device/OEM Camera qualification, production Glaze UI acceptance, application-specific Integral Platform System acceptance, signing/distribution readiness, Release Candidate status, Production Acceptance, or Stable status.

The public repository still has an unresolved recognized open-source license-selection blocker.

## 2026-09-23 — Current video/audio runtime candidate

Pull request #12, "Requalify Camera video/audio runtime after MediaStore finalization fix," remains open and unmerged.

Exact candidate head 194ae7626d1e1a67f2a0b45719987e52e14ca343 passed Platform Contract run 35643946740 and Android Foundation run 35643945957.

The Android Foundation run verified source/build validation, representative Android 16 preview/JPEG capture, and the video/audio emulator lane with explicit microphone permission, a non-empty published MP4, H.264 video, AAC audio, non-trivial duration, and preview restoration.

This is Development candidate evidence only. It does not establish physical-device/OEM audio quality, production UI, release, or Stable acceptance.

## 2026-09-23 — Platform Contract 0.4 candidate

Draft pull request #13 is stacked on PR #12 and remains unmerged.

It updates the candidate toward Platform Contract 0.4, the nine-system Integral Platform Systems model, and current Stable Glaze UI V1.6 / 1.6.0 authority while leaving Camera at Concept and keeping all application-specific integrations unaccepted.

Because PR #13 is not on main, Contract 0.4 and Glaze UI V1.6 are candidate-state repository changes rather than accepted Camera main state.

## 2026-09-23 — System-bar safe-area candidate

Draft pull request #14 is stacked on PR #13 and remains unmerged.

Exact head 2d44d1357e668d3a5237d63f1f1e3b5464532b3b passed Android Foundation run 35666905030.

The candidate keeps the engineering/status surface inside Android system-bar safe areas. It does not change Camera permission, capture, package, lifecycle, production, release, or Stable authority.

## 2026-09-16 — Video/audio implementation records reconciled

Pull request #9 reconciled repository documentation to the accepted PR #8 video/audio source/build foundation and was squash-merged as signed main commit d7a5ab0315a69975e3a8d0f5cf0b1df4f938bf24.

Post-merge Android Foundation run 35136970339 and Platform Contract run 35136971156 passed.

The evidence preserved the boundary that video/audio runtime and microphone routing were not yet qualified on accepted main.

## 2026-09-16 — Bounded video/audio source foundation accepted

Pull request #8 implemented bounded Camera2/MediaRecorder MP4 recording with H.264 video and AAC microphone audio, deterministic UTC MP4 naming, pending MediaStore video publication/cleanup, microphone hardware gating, explicit recording states, and just-in-time microphone authorization.

Candidate 86d2d63f373ddbe7df8e6681925fafb6596972f6 passed Android Foundation run 35101834080 and Platform Contract run 35101835061.

The PR was squash-merged as signed main commit f6d414049f6ad4792a70da00903fead403dde58c. Push-triggered Android Foundation run 35102550065 and Platform Contract run 35102551620 passed.

The runtime lane at that stage re-qualified preview/JPEG only and did not yet prove MediaRecorder or microphone runtime behavior.

## 2026-09-15 — First bounded JPEG capture accepted

Pull request #6 implemented one-shot JPEG still capture and pending MediaStore publication/finalization.

Candidate a378a17d3e6cf28a16521428fa3d77c1910e2e53 passed Android Foundation run 35083327911, including Android 16/API 36 JPEG + MediaStore runtime qualification, and Platform Contract run 35083328806.

It was squash-merged as signed main commit d5272da9877b47bfca1551784f32b1031a084a8e.

Post-merge Android Foundation run 35083832517 and Platform Contract run 35083833021 passed.

Pull request #7 then reconciled the repository records and was squash-merged as main commit 5017457a2e61716cf4eed6a0cae87922842eefc9 with successful post-merge validation.

## 2026-09-15 — Representative Camera2 preview qualified

Pull request #4 added exact-revision Android 16/API 36 virtual-camera preview qualification and was squash-merged as signed main commit 6dbd2c1a5c5e3fb523662f5a6e181c6dbf73644a.

The merged revision passed Android Foundation run 35021425774, including the preview runtime gate, and Platform Contract run 35021428000.

Pull request #5 reconciled documentation to that runtime evidence and was squash-merged as main commit 1687d81a65c037b46a25f98a23e5a0d1453e58de with successful Android Foundation and Platform Contract validation.

This evidence was representative emulator qualification, not physical-device qualification.

## 2026-09-15 — Native Android foundation accepted

Pull request #2 introduced the first native Android/Gradle application foundation under com.goreecloud.camera.

It established the Android 17/API 37 compile and target baseline, provisional API 29 minimum, Kotlin/AGP foundation, Camera2 capability discovery, deterministic camera selection, centralized camera-session ownership, camera permission flow, exact-revision CI, and a narrow privacy boundary without Internet, location, broad-storage, or media-library read permissions.

The PR was squash-merged as signed main commit 8de1eac6693f09ef52b0f12886108727352404e1.

Android Foundation run 35017167769 and Platform Contract run 35017168819 passed on the merged foundation.

## Initial repository-governance baseline

The first governed Camera repository baseline established the documentation system, product identity and version 0.1.0, lifecycle Concept, Platform Contract declaration, repository safety/privacy guidance, and feature-roadmap structure before the native Android source was introduced.

Historical repository and Drive documentation treated the Drive project specification and a synchronized Drive roadmap as canonical authorities. That authority model is superseded by the repository-native PROJECT-SPECIFICATIONS.md / PROJECT-RECORD.md model and the repository-native feature-state files.

## Product direction preserved from the migration sources

GoreeCloud Camera is intended as one coherent native capture platform spanning Professional Camera, Creative Camera, and Private Camera experiences.

Camera owns capture-session orchestration and source-media production. Gallery, Photos, Everkeep, and other downstream systems retain their own authority boundaries.

Core capture must remain useful offline and must not depend on an account, network connection, hosted AI, or commercial cloud service.

Camera must expose only hardware capabilities backed by effective capability profiles and qualification evidence.

Privacy, security, accessibility, recovery, thermal/storage behavior, diagnostics, Lenses, remote control, and release acceptance are first-class product requirements rather than optional documentation concerns.

## Authority transition

After this migration is accepted and verified on main:

- PROJECT-SPECIFICATIONS.md is the canonical Camera project specification.
- PROJECT-RECORD.md is the canonical significant project history/evidence record.
- IMPLEMENTED-FEATURES.md and PLANNED-FEATURES.md own feature lifecycle state.
- CHANGELOGS.md owns release- and repository-oriented change history.
- Google Drive no longer remains a parallel authoritative project specification or project record.

The former root SPECIFICATIONS.md is retired by the migration rather than retained as a competing canonical specification.
