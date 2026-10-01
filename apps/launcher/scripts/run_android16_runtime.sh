#!/usr/bin/env bash
set -euo pipefail

shard_count="${LAUNCHER_RUNTIME_SHARD_COUNT:-1}"
shard_index="${LAUNCHER_RUNTIME_SHARD_INDEX:-0}"

if ! [[ "$shard_count" =~ ^[1-9][0-9]*$ ]]; then
  echo "Invalid LAUNCHER_RUNTIME_SHARD_COUNT: $shard_count" >&2
  exit 2
fi
if ! [[ "$shard_index" =~ ^[0-9]+$ ]] || (( shard_index >= shard_count )); then
  echo "Invalid LAUNCHER_RUNTIME_SHARD_INDEX: $shard_index for count $shard_count" >&2
  exit 2
fi

runtime_dir="${GITHUB_WORKSPACE:?}/apps/launcher/ci-runtime/shard-$shard_index"
mkdir -p "$runtime_dir"

gradle_args=(
  gradle
  --project-dir "$GITHUB_WORKSPACE/apps/launcher"
  --no-daemon
  connectedDebugAndroidTest
)

if (( shard_count > 1 )); then
  gradle_args+=(
    "-Pandroid.testInstrumentationRunnerArguments.numShards=$shard_count"
    "-Pandroid.testInstrumentationRunnerArguments.shardIndex=$shard_index"
  )
fi

set +e
timeout --signal=TERM --kill-after=30s 15m "${gradle_args[@]}"
status=$?
set -e

printf 'gradle_exit_status=%s\nshard_index=%s\nshard_count=%s\n' \
  "$status" "$shard_index" "$shard_count" > "$runtime_dir/status.txt"

if [ "$status" -ne 0 ]; then
  adb logcat -d > "$runtime_dir/logcat.txt" || true
  adb shell dumpsys activity activities > "$runtime_dir/activity.txt" || true
  adb shell dumpsys window windows > "$runtime_dir/window.txt" || true
fi

exit "$status"
