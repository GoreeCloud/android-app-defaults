#!/usr/bin/env bash
set -euo pipefail

APK="${1:-}"
VERSION="${2:-}"
CLASSIFICATION="${3:-acceptance-candidate}"
OUTPUT="${4:-dist/validation/build-evidence.txt}"

fail() {
  printf 'GoreeCloud Gallery build evidence generation failed: %s\n' "$*" >&2
  exit 1
}

[ -n "$APK" ] || fail 'APK path is required'
[ -n "$VERSION" ] || fail 'version name is required'
[ -s "$APK" ] || fail "APK does not exist or is empty: $APK"

case "$CLASSIFICATION" in
  development|acceptance-candidate|signed-release-candidate|stable) ;;
  *) fail "unsupported release classification: $CLASSIFICATION" ;;
esac

mkdir -p "$(dirname "$OUTPUT")"

repository_commit="$(git rev-parse HEAD)"
apk_sha256="$(sha256sum "$APK" | awk '{print $1}')"
source_patch_line="$(find patches -mindepth 1 -maxdepth 1 -type d -name 'gc[0-9]*' -printf '%f\n' | sort -V | tail -n 1)"
[ -n "$source_patch_line" ] || fail 'could not determine the maintained GoreeCloud patch line'

cat > "$OUTPUT" <<EOF
schema_version=2
product=GoreeCloud Gallery
application_id=com.goreecloud.gallery
version_name=$VERSION
release_classification=$CLASSIFICATION
repository_commit=$repository_commit
validated_checkout_commit=$repository_commit
source_patch_line=$source_patch_line
fossify_gallery_commit=b28299dc33821eee8d108a9880ce87876cf31443
fossify_commons_commit=acfd352df1a1852d17a5f77def8b7ad6e522a5b6
workflow_run_id=${GITHUB_RUN_ID:-local}
workflow_run_number=${GITHUB_RUN_NUMBER:-local}
workflow_event=${GITHUB_EVENT_NAME:-local}
workflow_ref=${GITHUB_REF:-local}
runner_os=${RUNNER_OS:-local}
apk_filename=$(basename "$APK")
apk_sha256=$apk_sha256
internet_permission_expected=absent
glaze_ui_required=true
behavioral_tests_required=true
multi_user_model=android-os-user-profile-and-application-sandbox
stable_release_automatic=false
EOF

printf 'Wrote GoreeCloud Gallery build evidence: %s\n' "$OUTPUT"
