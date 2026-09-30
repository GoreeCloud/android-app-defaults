# GoreeCloud Gallery Release Evidence Template

## Purpose

Use this record for every GoreeCloud Gallery candidate considered for signed release-candidate or stable promotion. Keep evidence factual. Do not mark an item passed merely because a related task ran successfully.

Do not place reusable signing secrets, passwords, keystore bytes, personal media, private keys, or tokens in this record.

## Candidate identity

- GoreeCloud version name:
- Android version code:
- Release classification: Development / Acceptance Candidate / Signed Release Candidate / Stable
- GoreeCloud repository commit SHA:
- Fossify Gallery commit SHA: `b28299dc33821eee8d108a9880ce87876cf31443`
- Fossify Commons commit SHA: `acfd352df1a1852d17a5f77def8b7ad6e522a5b6`
- GitHub Actions workflow name:
- GitHub Actions run ID:
- GitHub Actions run number:
- Build date and time:
- APK filename:
- APK SHA-256:
- Signing-certificate SHA-256:

## Automated repository and source evidence

- Repository structure validation: Pass / Fail
- Repository security guardrails: Pass / Fail
- Exact revision checkout: Pass / Fail
- Historical patch materialization/provenance: Pass / Fail
- Exact upstream checkout: Pass / Fail
- gc patch chain application: Pass / Fail
- `git diff --check`: Pass / Fail
- GoreeCloud source invariants: Pass / Fail

Evidence notes:

## Test and lint evidence

- Android unit-test task result: Pass / Fail
- Did meaningful tests actually execute?: Yes / No
- If `NO-SOURCE`, explicitly record: No behavioral unit-test coverage was executed.
- GoreeCloud-owned automated test suites executed:
- Test report paths/artifacts:
- Android lint result: Pass / Fail
- Lint report path/artifact:
- Known accepted warnings or limitations:

## Packaged APK evidence

- Application ID equals `com.goreecloud.gallery`: Pass / Fail
- Version name matches candidate: Pass / Fail
- Expected debuggable state: Pass / Fail
- `android.permission.INTERNET` absent: Pass / Fail
- APK signature verifies: Pass / Fail
- Approved signer fingerprint matches when required: Pass / Fail / Not Applicable
- GoreeCloud notice present and version-correct: Pass / Fail
- Removed counterfeit-warning text absent from packaged DEX: Pass / Fail
- GPLv3 license evidence retained: Pass / Fail
- Final checksum generated after final signing state: Pass / Fail

## Android user/profile isolation evidence

- Device supports secondary user or managed/work profile: Yes / No
- Tested Android user/profile boundary: Pass / Fail / Not Available
- Application did not intentionally cross user/profile media boundary: Pass / Fail / Not Tested
- Notes:

## Device and permission evidence

Repeat this section for each representative device when necessary.

- Device manufacturer/model:
- Android version:
- Android security patch level, if recorded:
- Fresh install/startup: Pass / Fail
- Permission grant flow: Pass / Fail
- Permission denial flow: Pass / Fail
- Permission revocation/recovery flow: Pass / Fail
- Scoped-storage / SAF behavior: Pass / Fail / Not Applicable
- No network permission requested: Pass / Fail
- Notes:

## Media and destructive-operation evidence

Disposable copied media used: Yes / No

- Browse folders/media: Pass / Fail
- Open images: Pass / Fail
- Play videos: Pass / Fail
- Search: Pass / Fail
- Favorites: Pass / Fail
- Copy: Pass / Fail
- Move: Pass / Fail
- Rename where supported: Pass / Fail / Not Applicable
- Delete: Pass / Fail
- Trash/recycle-bin restore: Pass / Fail / Not Applicable
- Permanent delete of disposable media: Pass / Fail / Not Applicable
- Hidden/excluded folders: Pass / Fail
- Image editing/save behavior: Pass / Fail
- Share/open-with: Pass / Fail
- Background/rotation during safe operations: Pass / Fail
- Filesystem/media result independently verified where practical: Yes / No
- Notes:

## Glaze UI evidence

- Main folder view: Pass / Fail
- Opened media folder: Pass / Fail
- Rounded thumbnails and removed square controls: Pass / Fail
- Toolbar overflow light mode: Pass / Fail
- Toolbar overflow dark mode: Pass / Fail
- Dialog contrast: Pass / Fail
- Destructive-action presentation/confirmation: Pass / Fail
- No incorrect residual upstream product identity: Pass / Fail
- Screenshots or evidence references:

## Accessibility evidence

- TalkBack primary navigation/actions: Pass / Fail
- Icon-only accessible labels: Pass / Fail
- Large font: Pass / Fail
- Increased display size: Pass / Fail
- Practical touch targets: Pass / Fail
- Light/dark contrast: Pass / Fail
- Focus order: Pass / Fail
- Motion/animation does not block task completion: Pass / Fail
- Notes:

## Upgrade and recovery evidence

- Prior approved build/version:
- Same long-lived release certificate used: Yes / No / Not Applicable
- In-place upgrade: Pass / Fail
- Expected settings persisted: Pass / Fail
- Local media unchanged: Pass / Fail
- Approved signer fingerprint preserved: Pass / Fail
- Bad-release recovery procedure reviewed/tested: Pass / Fail
- Uninstall/reinstall settings impact documented: Pass / Fail
- Notes:

## Privacy and security evidence

- No analytics/advertising/tracking/cloud account/remote API introduced: Pass / Fail
- No reusable secret or signing material in candidate Git changes: Pass / Fail
- GitHub Actions least privilege: Pass / Fail
- Third-party Actions pinned to reviewed commit SHAs: Pass / Fail
- Checkout credentials not persisted: Pass / Fail
- Build artifacts contain only intended files: Pass / Fail
- Destructive testing used disposable media only: Pass / Fail
- Security findings requiring stable block:

## License and provenance evidence

- GNU GPL v3 obligations preserved: Pass / Fail
- GoreeCloud modification/provenance notice present: Pass / Fail
- Upstream revisions recorded: Pass / Fail
- Historical GoreeCloud patch provenance preserved: Pass / Fail
- Corresponding modified source reconstructable: Pass / Fail
- License/source review notes:

## Open release blockers

List every unresolved blocker. Include relevant issue numbers when available.

- 

## Stable-promotion decision

- All blocking checklist items complete: Yes / No
- P0/P1 release defect open: Yes / No
- Data-loss/destructive-operation blocker open: Yes / No
- Permission/privacy blocker open: Yes / No
- Signing blocker open: Yes / No
- Licensing/provenance blocker open: Yes / No
- Final signed APK SHA-256 recorded: Yes / No
- Final signer fingerprint recorded: Yes / No
- Final release evidence retained: Yes / No
- Deliberate stable publication approval granted: Yes / No

Decision: Keep as acceptance candidate / Keep as signed release candidate / Approve stable publication

Approved by:
Date and time:
Notes: