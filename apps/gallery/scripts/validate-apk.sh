#!/usr/bin/env bash
set -euo pipefail

APK="${1:-}"
EXPECTED_VERSION="${2:-1.0.0-gc.7}"
EXPECTED_APPLICATION_ID="${EXPECTED_APPLICATION_ID:-com.goreecloud.gallery}"
EXPECTED_DEBUGGABLE="${EXPECTED_DEBUGGABLE:-}"
EXPECTED_CERT_SHA256="${EXPECTED_CERT_SHA256:-}"
CHECKSUM_OUTPUT="${CHECKSUM_OUTPUT:-${APK}.sha256}"

fail() {
  printf 'GoreeCloud Gallery APK validation failed: %s\n' "$*" >&2
  exit 1
}

[ -n "$APK" ] || fail "usage: scripts/validate-apk.sh <apk> [expected-version]"
[ -s "$APK" ] || fail "APK does not exist or is empty: $APK"

find_android_tool() {
  local tool="$1"

  if command -v "$tool" >/dev/null 2>&1; then
    command -v "$tool"
    return 0
  fi

  if [ -n "${ANDROID_HOME:-}" ]; then
    local candidate
    while IFS= read -r candidate; do
      [ -x "$candidate" ] || continue
      printf '%s\n' "$candidate"
      return 0
    done < <(find "$ANDROID_HOME" -type f -name "$tool" 2>/dev/null | sort -V -r)
  fi

  return 1
}

APKANALYZER="$(find_android_tool apkanalyzer)" || fail "apkanalyzer is required for release validation"
APKSIGNER="$(find_android_tool apksigner)" || fail "apksigner is required for signature validation"

actual_application_id="$($APKANALYZER manifest application-id "$APK")"
[ "$actual_application_id" = "$EXPECTED_APPLICATION_ID" ] \
  || fail "application ID is $actual_application_id; expected $EXPECTED_APPLICATION_ID"

actual_version="$($APKANALYZER manifest version-name "$APK")"
[ "$actual_version" = "$EXPECTED_VERSION" ] \
  || fail "version name is $actual_version; expected $EXPECTED_VERSION"

if $APKANALYZER manifest permissions "$APK" | grep -Fq 'android.permission.INTERNET'; then
  fail "android.permission.INTERNET is present"
fi

if [ -n "$EXPECTED_DEBUGGABLE" ]; then
  case "$EXPECTED_DEBUGGABLE" in
    true|false) ;;
    *) fail "EXPECTED_DEBUGGABLE must be true or false" ;;
  esac

  actual_debuggable="$($APKANALYZER manifest debuggable "$APK")"
  [ "$actual_debuggable" = "$EXPECTED_DEBUGGABLE" ] \
    || fail "debuggable=$actual_debuggable; expected $EXPECTED_DEBUGGABLE"
fi

$APKSIGNER verify --verbose "$APK" >/dev/null \
  || fail "APK signature verification failed"

actual_cert_sha256=""
if [ -n "$EXPECTED_CERT_SHA256" ]; then
  if ! signer_output="$($APKSIGNER verify --print-certs "$APK" 2>&1)"; then
    fail "could not inspect APK signing certificate"
  fi

  cert_line="$(printf '%s\n' "$signer_output" | grep -i -m1 -E 'certificate.*sha-?256.*digest' || true)"
  [ -n "$cert_line" ] || fail "could not find signer certificate SHA-256 digest"

  actual_cert_sha256="$(
    printf '%s\n' "$cert_line" \
      | sed -E 's/^.*[Dd]igest:[[:space:]]*//' \
      | tr -cd '0-9A-Fa-f:' \
      | tr '[:upper:]' '[:lower:]'
  )"
  [ -n "$actual_cert_sha256" ] || fail "could not read signer certificate SHA-256 digest"

  normalized_expected="$(printf '%s' "$EXPECTED_CERT_SHA256" | tr '[:upper:]' '[:lower:]' | tr -d ':[:space:]')"
  normalized_actual="$(printf '%s' "$actual_cert_sha256" | tr -d ':[:space:]')"
  [ "$normalized_actual" = "$normalized_expected" ] \
    || fail "signer certificate does not match the approved release certificate"
fi

notice="$(unzip -p "$APK" assets/goreecloud_notice.txt 2>/dev/null || true)"
printf '%s\n' "$notice" | grep -Fq "GoreeCloud Gallery $EXPECTED_VERSION" \
  || fail "packaged GoreeCloud notice does not match $EXPECTED_VERSION"

mapfile -t dex_files < <(zipinfo -1 "$APK" | grep -E '^classes([0-9]+)?\.dex$' | sort)
[ "${#dex_files[@]}" -ge 1 ] || fail "APK contains no classes*.dex files"

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

for dex in "${dex_files[@]}"; do
  safe_name="${dex//\//_}"
  unzip -p "$APK" "$dex" > "$tmpdir/$safe_name"
  strings "$tmpdir/$safe_name" > "$tmpdir/$safe_name.strings"

  ! grep -Fq "You are using a fake version of the app" "$tmpdir/$safe_name.strings" \
    || fail "legacy counterfeit-build warning is packaged in $dex"
  ! grep -Fq "download the original one from www.fossify.org. Thanks" "$tmpdir/$safe_name.strings" \
    || fail "legacy Fossify download warning is packaged in $dex"
done

sha256sum "$APK" | tee "$CHECKSUM_OUTPUT"
printf 'Application ID: %s\n' "$actual_application_id"
printf 'Version: %s\n' "$actual_version"
if [ -n "$actual_cert_sha256" ]; then
  printf 'Signer certificate SHA-256: %s\n' "$actual_cert_sha256"
fi
printf 'GoreeCloud Gallery APK validation passed.\n'
