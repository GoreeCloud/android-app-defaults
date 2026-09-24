#!/usr/bin/env bash
set -euo pipefail

echo "Runtime APK artifact contents:"
find ci-apks -type f -maxdepth 4 -print | sort

APP_APK="$(find ci-apks/debug -maxdepth 1 -type f -name '*-debug.apk' | head -n 1)"
TEST_APK="$(find ci-apks/androidTest/debug -maxdepth 1 -type f -name '*-debug-androidTest.apk' | head -n 1)"

test -n "$APP_APK"
test -n "$TEST_APK"

echo "Android /data capacity before install:"
adb shell df -h /data

echo "Installing application APK: $APP_APK"
adb install -r "$APP_APK"

echo "Installing instrumentation APK: $TEST_APK"
adb install -r "$TEST_APK"

AAPT="$(find "$ANDROID_HOME/build-tools" -type f -name aapt | sort -V | tail -n 1)"
test -n "$AAPT"
TEST_PACKAGE="$("$AAPT" dump badging "$TEST_APK" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")"
test -n "$TEST_PACKAGE"

set +e
OUTPUT="$(adb shell am instrument -w "$TEST_PACKAGE/androidx.test.runner.AndroidJUnitRunner" 2>&1)"
STATUS=$?
set -e

printf '%s\n' "$OUTPUT"
test "$STATUS" -eq 0
printf '%s\n' "$OUTPUT" | grep -Eq 'OK \([1-9][0-9]* tests?\)'
