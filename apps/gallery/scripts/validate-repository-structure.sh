#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf 'GoreeCloud Gallery repository structure validation failed: %s\n' "$*" >&2
  exit 1
}

required_files=(
  README.md
  IMPLEMENTED-FEATURES.md
  PLANNED-FEATURES.md
  CHANGELOGS.md
  LICENSE
  SECURITY.md
  CONTRIBUTING.md
  NOTICE.md
  .gitignore
  docs/ARCHITECTURE.md
  docs/BUILD-AND-RELEASE.md
  docs/GLAZE-UI.md
  docs/GC17-DEVICE-ACCEPTANCE-FIXES.md
  docs/RELEASE-SIGNING.md
  docs/STABLE-SIGNING-RUNBOOK.md
  docs/REAL-DEVICE-ACCEPTANCE-RUNBOOK.md
  docs/STABLE-CANDIDATE-1.0.0.md
  docs/STABLE-RELEASE-CHECKLIST.md
  docs/RELEASE-EVIDENCE-TEMPLATE.md
  docs/REPOSITORY-READINESS.md
  patches/gc9/build_goreecloud_gallery_gc9.py
  patches/gc10/build_goreecloud_gallery_gc10.py
  patches/gc11/build_goreecloud_gallery_gc11.py
  patches/gc12/build_goreecloud_gallery_gc12.py
  patches/gc13/build_goreecloud_gallery_gc13.py
  patches/gc14/build_goreecloud_gallery_gc14.py
  patches/gc15/build_goreecloud_gallery_gc15.py
  patches/gc16/build_goreecloud_gallery_gc16.py
  patches/gc17/build_goreecloud_gallery_gc17.py
  scripts/materialize-patches.sh
  scripts/reconstruct-source.sh
  scripts/validate-apk.sh
  scripts/validate-repository-security.sh
  scripts/validate-repository-structure.sh
  scripts/validate-source-invariants.sh
  scripts/write-build-evidence.sh
)

for path in "${required_files[@]}"; do
  [ -s "$path" ] || fail "required file is missing or empty: $path"
done

repository_root="$(git rev-parse --show-toplevel 2>/dev/null || pwd -P)"
gallery_root="$(pwd -P)"

if [ "$repository_root" = "$gallery_root" ]; then
  standalone_control_files=(
    .github/CODEOWNERS
    .github/dependabot.yml
    .github/pull_request_template.md
    .github/workflows/build-and-validate.yml
    .github/workflows/build-signed-release-candidate.yml
  )
  for path in "${standalone_control_files[@]}"; do
    [ -s "$path" ] || fail "standalone control-plane file is missing or empty: $path"
  done
  grep -Fq '@GoreeCloud' .github/CODEOWNERS ||
    fail 'standalone CODEOWNERS does not identify GoreeCloud review ownership'
else
  migrated_ci="$repository_root/.github/workflows/migrated-android-apps-ci.yml"
  [ -s "$migrated_ci" ] || fail 'monorepo migrated Android CI workflow is missing'
  grep -Fq 'gallery:' "$migrated_ci" || fail 'monorepo CI does not define the Gallery validation job'
  grep -Fq 'working-directory: apps/gallery/native' "$migrated_ci" ||
    fail 'monorepo CI does not target the Gallery native project'
fi

[ ! -e FEATURE-ROADMAP.md ] || fail 'retired FEATURE-ROADMAP.md must not exist at repository root'
[ ! -e CHANGELOG.md ] || fail 'retired singular CHANGELOG.md must not exist at repository root'

for patch_line in gc1 gc2 gc3 gc4 gc5 gc6 gc7 gc8 gc9 gc10 gc11 gc12 gc13 gc14 gc15 gc16 gc17; do
  [ -d "patches/$patch_line" ] || fail "required patch directory is missing: patches/$patch_line"
done

grep -Fq 'GoreeCloud Gallery' README.md || fail 'README does not identify GoreeCloud Gallery'
grep -Fq 'com.goreecloud.gallery' README.md || fail 'README does not record the application ID'
grep -Fq 'Glaze UI' README.md || fail 'README does not record the Glaze UI requirement'
grep -Fq 'GNU GENERAL PUBLIC LICENSE' LICENSE || fail 'root LICENSE is not the GNU GPL license text'
grep -Fq 'Version 3, 29 June 2007' LICENSE || fail 'root LICENSE does not identify GNU GPL version 3'
grep -Fq 'GNU General Public License' NOTICE.md || fail 'NOTICE does not record the GPL license boundary'
grep -Fq 'b28299dc33821eee8d108a9880ce87876cf31443' NOTICE.md || fail 'NOTICE does not record the pinned Fossify Gallery revision'
grep -Fq 'acfd352df1a1852d17a5f77def8b7ad6e522a5b6' NOTICE.md || fail 'NOTICE does not record the pinned Fossify Commons revision'

grep -Fq 'Android user / profile boundary' docs/ARCHITECTURE.md || fail 'architecture does not document Android user/profile isolation'
grep -Fq 'Glaze UI architecture' docs/ARCHITECTURE.md || fail 'architecture does not document the Glaze UI layer'
grep -Fq 'GoreeCloud Gallery Glaze UI Contract' docs/GLAZE-UI.md || fail 'Glaze contract does not identify its Gallery scope'
grep -Fq 'Target design system: **Glaze V1.7 / 1.7.0**' docs/GLAZE-UI.md || fail 'Glaze target version is not documented'
grep -Fq '7c4ded83d7a8725165bb6a55dfb175667cc9589e' docs/GLAZE-UI.md || fail 'current Glaze qualification anchor is not documented'
grep -Fq 'a7180679ea851389e0f3004515f9a25f420e716d' docs/GLAZE-UI.md || fail 'inherited accepted Glaze runtime release source is not documented'
grep -Fq 'Current first-party native source baseline: **V1.7**' docs/GLAZE-UI.md || fail 'current native Glaze baseline is not documented'
grep -Fq 'Retained V1.7 dev.47 / Section 48 Development behavior included: **no**' docs/GLAZE-UI.md || fail 'excluded Glaze Development behavior is not documented'
grep -Fq 'Historical transitional implementation line: `gc.16`' docs/GLAZE-UI.md || fail 'historical transitional Glaze baseline is not documented'
grep -Fq 'No permanent Glaze UI exception is approved' docs/GLAZE-UI.md || fail 'Glaze UI exception boundary is not documented'
grep -Fq 'meaningful GoreeCloud-owned JVM tests actually execute' docs/GLAZE-UI.md || fail 'Glaze UI contract does not preserve behavioral-test evidence requirements'
grep -Fq 'The third finding is a functional defect' docs/GC17-DEVICE-ACCEPTANCE-FIXES.md || fail 'gc.17 functional regression record is incomplete'
grep -Fq 'deleteEmptyFolders' docs/GC17-DEVICE-ACCEPTANCE-FIXES.md || fail 'gc.17 explicit delete regression is not documented'
grep -Fq 'overflow text is readable' docs/GC17-DEVICE-ACCEPTANCE-FIXES.md || fail 'gc.17 popup device gate is not documented'
grep -Fq 'GoreeCloud Gallery Stable Signing Runbook' docs/STABLE-SIGNING-RUNBOOK.md || fail 'stable signing runbook does not identify its Gallery scope'
grep -Fq 'GoreeCloud Gallery Real-Device Acceptance Runbook' docs/REAL-DEVICE-ACCEPTANCE-RUNBOOK.md || fail 'real-device runbook does not identify its Gallery scope'
grep -Fq 'GoreeCloud Gallery 1.0.0 Stable Candidate' docs/STABLE-CANDIDATE-1.0.0.md || fail 'Stable candidate contract does not identify the final candidate'
grep -Fq 'Stable release: Not approved' docs/REPOSITORY-READINESS.md || fail 'repository readiness record does not preserve the stable-release boundary'

grep -Fq 'VERSION_NAME = "1.0.0"' patches/gc9/build_goreecloud_gallery_gc9.py || fail 'gc.9 does not set the final semantic version'
grep -Fq 'VERSION_CODE = "10016"' patches/gc16/build_goreecloud_gallery_gc16.py || fail 'gc.16 historical version identity is missing'
grep -Fq 'compact_dialog' patches/gc16/build_goreecloud_gallery_gc16.py || fail 'gc.16 compact-dialog contract is missing'
grep -Fq 'fillViewport' patches/gc16/build_goreecloud_gallery_gc16.py || fail 'gc.16 dialog viewport contract is missing'
grep -Fq 'VERSION_CODE = "10017"' patches/gc17/build_goreecloud_gallery_gc17.py || fail 'gc.17 does not set the current test-build version code'
grep -Fq 'patch_explicit_folder_delete' patches/gc17/build_goreecloud_gallery_gc17.py || fail 'gc.17 explicit folder-delete fix is missing'
grep -Fq 'patch_popup_contrast' patches/gc17/build_goreecloud_gallery_gc17.py || fail 'gc.17 popup contrast fix is missing'
grep -Fq 'patch_confirm_delete_layout' patches/gc17/build_goreecloud_gallery_gc17.py || fail 'gc.17 destructive-dialog geometry fix is missing'

if git ls-files -z | grep -zE '\.(apk|aab|jks|keystore|p12|pfx|pem|key|der)$' >/dev/null; then
  fail 'generated package or key/certificate-container material is tracked in Git'
fi

for generated in .build upstream-gallery upstream-commons dist dex-validation; do
  if git ls-files "$generated/**" | grep -q .; then
    fail "generated build path contains tracked files: $generated"
  fi
done

printf 'GoreeCloud Gallery repository structure validation passed.\n'
