#!/usr/bin/env bash
# File internal version: 0.4.0
set -euo pipefail

readonly APP_ID="com.goreecloud.camera"
readonly ACTIVITY="${APP_ID}/.MainActivity"
readonly EVIDENCE_ROOT="runtime-capture-evidence"
readonly MEDIA_URI="content://media/external_primary/images/media"
readonly SHUTTER_CONTENT_DESCRIPTION="Capture a JPEG photo to shared media"
preview_ready=0
photo_saved=0
media_verified=0
jpeg_verified=0
first_use_guidance_verified=0
photo_name=""
media_id=""

rm -rf "$EVIDENCE_ROOT"
mkdir -p "$EVIDENCE_ROOT"

dump_window() {
  adb shell uiautomator dump /sdcard/goreecloud-camera-window.xml >/dev/null 2>&1 || true
  adb pull /sdcard/goreecloud-camera-window.xml "$EVIDENCE_ROOT/window.xml" >/dev/null 2>&1 || true
}

dismiss_external_system_anr_dialog() {
  dump_window

  local tap_coordinates
  tap_coordinates="$(
    python3 - "$EVIDENCE_ROOT/window.xml" <<'PY' || true
import re
import sys
import xml.etree.ElementTree as ET

try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    raise SystemExit(0)

alert_title = None
close_button = None

for node in root.iter("node"):
    package_name = node.attrib.get("package")
    resource_id = node.attrib.get("resource-id")
    text = node.attrib.get("text", "")

    if package_name == "android" and resource_id == "android:id/alertTitle":
        alert_title = text

    if (
        package_name == "android"
        and resource_id == "android:id/aerr_close"
        and node.attrib.get("clickable") == "true"
        and node.attrib.get("enabled") == "true"
    ):
        close_button = node

if not alert_title or not alert_title.endswith(" isn't responding") or close_button is None:
    raise SystemExit(0)

bounds = close_button.attrib.get("bounds", "")
match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
if not match:
    raise SystemExit(0)

x1, y1, x2, y2 = map(int, match.groups())
print((x1 + x2) // 2, (y1 + y2) // 2)
PY
  )"

  if [ -n "$tap_coordinates" ]; then
    read -r tap_x tap_y <<<"$tap_coordinates"
    echo "Dismissing unrelated Android system ANR dialog before Camera qualification: x=$tap_x y=$tap_y"
    adb shell input tap "$tap_x" "$tap_y" || true
    sleep 1
  fi
}

capture_evidence() {
  local status=$?
  set +e

  dump_window
  adb exec-out screencap -p > "$EVIDENCE_ROOT/capture.png" 2>/dev/null || true
  adb shell dumpsys media.camera > "$EVIDENCE_ROOT/media-camera.txt" 2>/dev/null || true
  adb shell dumpsys package "$APP_ID" > "$EVIDENCE_ROOT/package.txt" 2>/dev/null || true
  adb shell cmd appops get "$APP_ID" > "$EVIDENCE_ROOT/appops.txt" 2>/dev/null || true
  adb shell content query --uri "$MEDIA_URI" --projection _id:_display_name:mime_type:relative_path:_size > "$EVIDENCE_ROOT/media-store.txt" 2>&1 || true
  adb logcat -d -v threadtime > "$EVIDENCE_ROOT/logcat.txt" 2>/dev/null || true

  local qualification_result="failed"
  if [ "$status" -eq 0 ] &&
     [ "$preview_ready" -eq 1 ] &&
     [ "$photo_saved" -eq 1 ] &&
     [ "$media_verified" -eq 1 ] &&
     [ "$jpeg_verified" -eq 1 ] &&
     [ "$first_use_guidance_verified" -eq 1 ]; then
    qualification_result="passed"
  fi

  cat > "$EVIDENCE_ROOT/RUNTIME-PROVENANCE.txt" <<PROVENANCE
repository=GoreeCloud/android-app-defaults
source_sha=${SOURCE_SHA:?SOURCE_SHA is required}
product_version=0.1.0
lifecycle=Concept
runtime=Android-16-API-36-emulator
camera_source=Android-emulated-back-camera
permission_scope=android.permission.CAMERA-only
expected_preview_state=previewing
expected_capture_state=photo-saved-and-published
media_destination=MediaStore.Images/external_primary/DCIM/GoreeCloud Camera
photo_display_name=$photo_name
media_id=$media_id
evidence_role=representative-emulator-preview-and-still-capture-qualification
qualification_result=$qualification_result
exit_status=$status
first_use_guidance_resume_replay_and_hint_controls_verified=$first_use_guidance_verified
physical_device_qualification=false
stable_release_authority=none
PROVENANCE

  trap - EXIT
  exit "$status"
}
trap capture_evidence EXIT

gradle --no-daemon --stacktrace assembleDebug
APK="$(find app/build/outputs/apk/debug -type f -name '*.apk' -print -quit)"
test -n "$APK"

adb install -r "$APK"
adb shell pm grant "$APP_ID" android.permission.CAMERA
adb shell am force-stop "$APP_ID"
adb shell input keyevent KEYCODE_WAKEUP || true
adb shell wm dismiss-keyguard || true
adb shell am start -W -n "$ACTIVITY"
bash scripts/qualify_first_use_guidance.sh "$APP_ID"
first_use_guidance_verified=1

for attempt in $(seq 1 45); do
  dismiss_external_system_anr_dialog
  dump_window
  if grep -q 'text="Session: previewing' "$EVIDENCE_ROOT/window.xml" 2>/dev/null; then
    preview_ready=1
    break
  fi
  sleep 1
done

if [ "$preview_ready" -ne 1 ]; then
  echo "Camera preview did not reach PREVIEWING state." >&2
  cat "$EVIDENCE_ROOT/window.xml" >&2 2>/dev/null || true
  exit 1
fi

grep -Eq 'text="Detected camera devices: [1-9][0-9]*"' "$EVIDENCE_ROOT/window.xml"
cp "$EVIDENCE_ROOT/window.xml" "$EVIDENCE_ROOT/window-before-capture.xml"

read -r tap_x tap_y < <(
  python3 - "$EVIDENCE_ROOT/window-before-capture.xml" "$SHUTTER_CONTENT_DESCRIPTION" "$APP_ID" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
content_description = sys.argv[2]
app_id = sys.argv[3]
for node in root.iter("node"):
    if (
        node.attrib.get("package") == app_id
        and node.attrib.get("content-desc") == content_description
        and node.attrib.get("clickable") == "true"
        and node.attrib.get("enabled") == "true"
    ):
        bounds = node.attrib.get("bounds", "")
        match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            raise SystemExit(0)
raise SystemExit("Enabled Camera capture button with expected accessibility description not found")
PY
)
printf 'x=%s\ny=%s\n' "$tap_x" "$tap_y" > "$EVIDENCE_ROOT/shutter-tap.txt"

adb shell input tap "$tap_x" "$tap_y"

for attempt in $(seq 1 45); do
  dump_window
  photo_name="$(
    python3 - "$EVIDENCE_ROOT/window.xml" <<'PY' || true
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
prefix = "Photo saved: "
for node in root.iter("node"):
    text = node.attrib.get("text", "")
    if text.startswith(prefix):
        print(text[len(prefix):])
        break
PY
  )"
  if [[ "$photo_name" == GCAM_*.jpg ]]; then
    photo_saved=1
    break
  fi
  sleep 1
done

if [ "$photo_saved" -ne 1 ]; then
  echo "Camera did not report a saved photo." >&2
  cat "$EVIDENCE_ROOT/window.xml" >&2 2>/dev/null || true
  exit 1
fi

adb shell content query \
  --uri "$MEDIA_URI" \
  --projection _id:_display_name:mime_type:relative_path:_size \
  > "$EVIDENCE_ROOT/media-store.txt"

media_id="$(
  python3 - "$EVIDENCE_ROOT/media-store.txt" "$photo_name" <<'PY'
import re
import sys

text = open(sys.argv[1], encoding="utf-8").read()
name = sys.argv[2]
for line in text.splitlines():
    if f"_display_name={name}" not in line:
        continue
    if "mime_type=image/jpeg" not in line:
        continue
    if "relative_path=DCIM/GoreeCloud Camera/" not in line:
        continue
    size_match = re.search(r"_size=(\d+)", line)
    id_match = re.search(r"(?:^|[ ,])_id=(\d+)", line)
    if size_match and int(size_match.group(1)) > 0 and id_match:
        print(id_match.group(1))
        break
PY
)"

if [ -z "$media_id" ]; then
  echo "Published MediaStore JPEG row was not found or had zero size." >&2
  cat "$EVIDENCE_ROOT/media-store.txt" >&2
  exit 1
fi
media_verified=1

adb exec-out content read --uri "${MEDIA_URI}/${media_id}" > "$EVIDENCE_ROOT/captured.jpg"
python3 - "$EVIDENCE_ROOT/captured.jpg" <<'PY'
import sys

data = open(sys.argv[1], "rb").read()
if len(data) < 2 or data[:2] != b"\xff\xd8":
    raise SystemExit("Captured MediaStore payload is not a JPEG")
PY
jpeg_verified=1

echo "Camera2 preview and JPEG still capture reached a published MediaStore artifact on the Android 16 emulated-camera runtime."
