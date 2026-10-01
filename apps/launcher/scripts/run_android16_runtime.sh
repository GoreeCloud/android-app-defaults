#!/usr/bin/env bash
set -euo pipefail

runtime_dir="${GITHUB_WORKSPACE:?}/apps/launcher/ci-runtime"
mkdir -p "$runtime_dir"

set +e
timeout --signal=TERM --kill-after=30s 20m   gradle --project-dir "$GITHUB_WORKSPACE/apps/launcher" --no-daemon connectedDebugAndroidTest
status=$?
set -e

printf 'gradle_exit_status=%s\n' "$status" > "$runtime_dir/status.txt"

if [ "$status" -ne 0 ]; then
  adb logcat -d > "$runtime_dir/logcat.txt" || true
  adb shell dumpsys activity activities > "$runtime_dir/activity.txt" || true
  adb shell dumpsys window windows > "$runtime_dir/window.txt" || true
fi

exit "$status"
