#!/usr/bin/env bash
set -euo pipefail

echo "Clock runtime APK artifact contents:"
find ci-clock-apks -type f -maxdepth 4 -print | sort

APP_APK="$(find ci-clock-apks/debug -maxdepth 1 -type f -name '*-debug.apk' | head -n 1)"
TEST_APK="$(find ci-clock-apks/androidTest/debug -maxdepth 1 -type f -name '*-debug-androidTest.apk' | head -n 1)"

test -n "$APP_APK"
test -n "$TEST_APK"

echo "Installing Clock application APK: $APP_APK"
adb install -r "$APP_APK"

echo "Installing Clock instrumentation APK: $TEST_APK"
adb install -r "$TEST_APK"

AAPT="$(find "$ANDROID_HOME/build-tools" -type f -name aapt | sort -V | tail -n 1)"
test -n "$AAPT"
TEST_PACKAGE="$("$AAPT" dump badging "$TEST_APK" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")"
APP_PACKAGE="$("$AAPT" dump badging "$APP_APK" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")"
test -n "$TEST_PACKAGE"
test -n "$APP_PACKAGE"

adb logcat -c

set +e
OUTPUT="$(adb shell am instrument -w "$TEST_PACKAGE/androidx.test.runner.AndroidJUnitRunner" 2>&1)"
STATUS=$?
set -e

printf '%s\n' "$OUTPUT"

if [ "$STATUS" -ne 0 ] || ! printf '%s\n' "$OUTPUT" | grep -Eq 'OK \([1-9][0-9]* tests?\)'; then
    echo "Clock instrumentation did not complete successfully. Recent Android logcat:"
    adb logcat -d -v threadtime | tail -n 1500
    exit 1
fi

echo "Collecting rendered GoreeCloud Clock evidence."
rm -rf ci-clock-screenshots
mkdir -p ci-clock-screenshots
for screenshot in \
  clock-digital \
  clock-analog \
  alarms-empty \
  alarm-editor \
  timer-empty \
  timer-editor \
  stopwatch \
  world-clock \
  world-clock-picker \
  settings \
  settings-dark \
  clock-digital-dark
do
    destination="ci-clock-screenshots/${screenshot}.png"
    adb exec-out run-as "$APP_PACKAGE" cat "files/visual-evidence/${screenshot}.png" > "$destination"
    test -s "$destination"
done

echo "Clock rendered evidence files:"
find ci-clock-screenshots -maxdepth 1 -type f -name '*.png' -print | sort
