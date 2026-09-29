#!/usr/bin/env bash
# File internal version: 0.3.0
set -euo pipefail

readonly APP_ID="com.goreecloud.camera"
readonly ACTIVITY="${APP_ID}/.MainActivity"
readonly EVIDENCE_ROOT="runtime-video-audio-evidence"
readonly VIDEO_URI="content://media/external_primary/video/media"
readonly RECORD_CONTENT_DESCRIPTION="Record an MP4 video with microphone audio"
readonly STOP_CONTENT_DESCRIPTION="Stop and save the active video and microphone recording"

preview_ready=0
permission_denied_before_intent=0
permission_prompt_observed=0
permission_granted_after_prompt=0
no_auto_start_verified=0
recording_started=0
video_saved=0
media_verified=0
video_track_verified=0
audio_track_verified=0
preview_restored=0
first_use_guidance_verified=0
video_name=""
media_id=""

rm -rf "$EVIDENCE_ROOT"
mkdir -p "$EVIDENCE_ROOT"

dump_window_to() {
  local destination="$1"
  adb shell uiautomator dump /sdcard/goreecloud-camera-window.xml >/dev/null 2>&1 || true
  adb pull /sdcard/goreecloud-camera-window.xml "$destination" >/dev/null 2>&1 || true
}

current_window() {
  dump_window_to "$EVIDENCE_ROOT/window.xml"
}

dismiss_external_system_anr_dialog() {
  current_window

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

match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", close_button.attrib.get("bounds", ""))
if not match:
    raise SystemExit(0)

x1, y1, x2, y2 = map(int, match.groups())
print((x1 + x2) // 2, (y1 + y2) // 2)
PY
  )"

  if [ -n "$tap_coordinates" ]; then
    read -r tap_x tap_y <<<"$tap_coordinates"
    echo "Dismissing unrelated Android system ANR dialog before Camera video/audio qualification: x=$tap_x y=$tap_y"
    adb shell input tap "$tap_x" "$tap_y" || true
    sleep 1
  fi
}

microphone_permission_state() {
  local destination="$1"
  adb shell dumpsys package "$APP_ID" > "$destination"
  python3 - "$destination" <<'PY'
import re
import sys

text = open(sys.argv[1], encoding="utf-8", errors="replace").read()
match = re.search(r"android\.permission\.RECORD_AUDIO:\s+granted=(true|false)", text)
if not match:
    raise SystemExit("RECORD_AUDIO runtime permission state was not found in dumpsys package output")
print("granted" if match.group(1) == "true" else "denied")
PY
}

capture_evidence() {
  local status=$?
  set +e

  current_window
  adb exec-out screencap -p > "$EVIDENCE_ROOT/video-audio.png" 2>/dev/null || true
  adb shell dumpsys media.camera > "$EVIDENCE_ROOT/media-camera.txt" 2>/dev/null || true
  adb shell dumpsys media.audio_flinger > "$EVIDENCE_ROOT/audio-flinger.txt" 2>/dev/null || true
  adb shell dumpsys audio > "$EVIDENCE_ROOT/audio-service.txt" 2>/dev/null || true
  adb shell dumpsys package "$APP_ID" > "$EVIDENCE_ROOT/package.txt" 2>/dev/null || true
  adb shell cmd appops get "$APP_ID" > "$EVIDENCE_ROOT/appops.txt" 2>/dev/null || true
  adb shell pm list features > "$EVIDENCE_ROOT/device-features.txt" 2>/dev/null || true
  adb shell content query --uri "$VIDEO_URI" --projection _id:_display_name:mime_type:relative_path:_size > "$EVIDENCE_ROOT/video-media-store.txt" 2>&1 || true
  adb logcat -d -v threadtime > "$EVIDENCE_ROOT/logcat.txt" 2>/dev/null || true

  local qualification_result="failed"
  if [ "$status" -eq 0 ] &&
     [ "$preview_ready" -eq 1 ] &&
     [ "$permission_denied_before_intent" -eq 1 ] &&
     [ "$permission_prompt_observed" -eq 1 ] &&
     [ "$permission_granted_after_prompt" -eq 1 ] &&
     [ "$no_auto_start_verified" -eq 1 ] &&
     [ "$recording_started" -eq 1 ] &&
     [ "$video_saved" -eq 1 ] &&
     [ "$media_verified" -eq 1 ] &&
     [ "$video_track_verified" -eq 1 ] &&
     [ "$audio_track_verified" -eq 1 ] &&
     [ "$preview_restored" -eq 1 ] &&
     [ "$first_use_guidance_verified" -eq 1 ]; then
    qualification_result="passed"
  fi

  cat > "$EVIDENCE_ROOT/RUNTIME-PROVENANCE.txt" <<PROVENANCE
repository=GoreeCloud/goreecloud-camera
source_sha=${SOURCE_SHA:?SOURCE_SHA is required}
product_version=0.1.0
lifecycle=Concept
runtime=Android-16-API-36-emulator
camera_source=Android-emulated-back-camera
host_audio_source=local-PulseAudio-null-sink-monitor
permission_scope=android.permission.CAMERA-plus-just-in-time-android.permission.RECORD_AUDIO
microphone_permission_denied_before_record_intent=$permission_denied_before_intent
microphone_permission_prompt_observed=$permission_prompt_observed
microphone_permission_granted_after_prompt=$permission_granted_after_prompt
recording_did_not_auto_start_after_permission_grant=$no_auto_start_verified
recording_started=$recording_started
expected_recording_format=MP4-H264-AAC
video_display_name=$video_name
media_id=$media_id
video_track_verified=$video_track_verified
audio_track_verified=$audio_track_verified
preview_restored=$preview_restored
evidence_role=representative-emulator-video-audio-runtime-qualification
microphone_input_signal_qualification=not-asserted
first_use_guidance_resume_replay_and_hint_controls_verified=$first_use_guidance_verified
physical_device_qualification=false
stable_release_authority=none
qualification_result=$qualification_result
exit_status=$status
PROVENANCE

  trap - EXIT
  exit "$status"
}
trap capture_evidence EXIT

command -v ffprobe >/dev/null 2>&1 || {
  echo "ffprobe is required for MP4 track qualification." >&2
  exit 1
}

gradle --no-daemon --stacktrace assembleDebug
APK="$(find app/build/outputs/apk/debug -type f -name '*.apk' -print -quit)"
test -n "$APK"

adb install -r "$APK"
adb shell pm grant "$APP_ID" android.permission.CAMERA
adb shell pm revoke "$APP_ID" android.permission.RECORD_AUDIO >/dev/null 2>&1 || true

adb shell pm list features > "$EVIDENCE_ROOT/device-features-before.txt"
if ! grep -q 'feature:android.hardware.microphone' "$EVIDENCE_ROOT/device-features-before.txt"; then
  echo "Representative runtime does not advertise android.hardware.microphone." >&2
  exit 1
fi

microphone_permission_before="$(microphone_permission_state "$EVIDENCE_ROOT/package-before-record-intent.txt")"
printf '%s\n' "$microphone_permission_before" > "$EVIDENCE_ROOT/microphone-permission-before.txt"
if [ "$microphone_permission_before" != "denied" ]; then
  echo "Microphone permission must be denied before deliberate recording intent; observed: $microphone_permission_before" >&2
  exit 1
fi
permission_denied_before_intent=1

adb shell am force-stop "$APP_ID"
adb shell input keyevent KEYCODE_WAKEUP || true
adb shell wm dismiss-keyguard || true
adb shell am start -W -n "$ACTIVITY"
bash scripts/qualify_first_use_guidance.sh "$APP_ID"
first_use_guidance_verified=1

for attempt in $(seq 1 45); do
  dismiss_external_system_anr_dialog
  current_window
  if grep -q 'text="Session: previewing' "$EVIDENCE_ROOT/window.xml" 2>/dev/null; then
    preview_ready=1
    break
  fi
  sleep 1
done

if [ "$preview_ready" -ne 1 ]; then
  echo "Camera preview did not reach PREVIEWING state before video qualification." >&2
  cat "$EVIDENCE_ROOT/window.xml" >&2 2>/dev/null || true
  exit 1
fi

cp "$EVIDENCE_ROOT/window.xml" "$EVIDENCE_ROOT/window-before-record-intent.xml"

read -r record_x record_y < <(
  python3 - "$EVIDENCE_ROOT/window-before-record-intent.xml" "$RECORD_CONTENT_DESCRIPTION" "$APP_ID" <<'PY'
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
raise SystemExit("Enabled Camera Record-video control was not found")
PY
)
printf 'x=%s\ny=%s\n' "$record_x" "$record_y" > "$EVIDENCE_ROOT/record-intent-tap.txt"
adb shell input tap "$record_x" "$record_y"

permission_tap=""
for attempt in $(seq 1 20); do
  dismiss_external_system_anr_dialog
  dump_window_to "$EVIDENCE_ROOT/window-permission.xml"
  permission_tap="$(
    python3 - "$EVIDENCE_ROOT/window-permission.xml" <<'PY' 2>/dev/null || true
import re
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
preferred_ids = (
    "permission_allow_foreground_only_button",
    "permission_allow_one_time_button",
)
preferred_text = (
    "While using the app",
    "Only this time",
    "Allow",
)

for suffix in preferred_ids:
    for node in root.iter("node"):
        if node.attrib.get("enabled") != "true" or node.attrib.get("clickable") != "true":
            continue
        if not node.attrib.get("resource-id", "").endswith(suffix):
            continue
        bounds = node.attrib.get("bounds", "")
        match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            raise SystemExit(0)

for label in preferred_text:
    for node in root.iter("node"):
        if node.attrib.get("enabled") != "true" or node.attrib.get("clickable") != "true":
            continue
        if node.attrib.get("text") != label:
            continue
        package = node.attrib.get("package", "")
        if "permissioncontroller" not in package:
            continue
        bounds = node.attrib.get("bounds", "")
        match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            raise SystemExit(0)
PY
  )"
  if [ -n "$permission_tap" ]; then
    permission_prompt_observed=1
    break
  fi
  sleep 1
done

if [ "$permission_prompt_observed" -ne 1 ]; then
  echo "Microphone runtime-permission prompt was not observed after deliberate Record-video intent." >&2
  cat "$EVIDENCE_ROOT/window-permission.xml" >&2 2>/dev/null || true
  exit 1
fi

read -r permission_x permission_y <<< "$permission_tap"
printf 'x=%s\ny=%s\n' "$permission_x" "$permission_y" > "$EVIDENCE_ROOT/microphone-permission-tap.txt"
adb shell input tap "$permission_x" "$permission_y"

microphone_permission_after="unknown"
for attempt in $(seq 1 20); do
  microphone_permission_after="$(microphone_permission_state "$EVIDENCE_ROOT/package-after-permission.txt")"
  if [ "$microphone_permission_after" = "granted" ]; then
    permission_granted_after_prompt=1
    break
  fi
  sleep 1
done
printf '%s\n' "$microphone_permission_after" > "$EVIDENCE_ROOT/microphone-permission-after.txt"

if [ "$permission_granted_after_prompt" -ne 1 ]; then
  echo "Microphone permission was not granted through the observed runtime prompt." >&2
  exit 1
fi

for attempt in $(seq 1 20); do
  dismiss_external_system_anr_dialog
  current_window
  if grep -q 'text="Session: previewing' "$EVIDENCE_ROOT/window.xml" 2>/dev/null &&
     ! grep -q 'text="Session: recording' "$EVIDENCE_ROOT/window.xml" 2>/dev/null; then
    no_auto_start_verified=1
    break
  fi
  sleep 1
done

if [ "$no_auto_start_verified" -ne 1 ]; then
  echo "Recording must not auto-start after microphone permission grant." >&2
  exit 1
fi
cp "$EVIDENCE_ROOT/window.xml" "$EVIDENCE_ROOT/window-after-permission-before-second-intent.xml"

read -r record_x record_y < <(
  python3 - "$EVIDENCE_ROOT/window-after-permission-before-second-intent.xml" "$RECORD_CONTENT_DESCRIPTION" "$APP_ID" <<'PY'
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
        match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            raise SystemExit(0)
raise SystemExit("Record-video control was not available after microphone permission grant")
PY
)
adb shell input tap "$record_x" "$record_y"

for attempt in $(seq 1 30); do
  dismiss_external_system_anr_dialog
  current_window
  if grep -q 'text="Session: recording' "$EVIDENCE_ROOT/window.xml" 2>/dev/null; then
    recording_started=1
    break
  fi
  sleep 1
done

if [ "$recording_started" -ne 1 ]; then
  echo "Video/audio recording did not reach RECORDING state." >&2
  cat "$EVIDENCE_ROOT/window.xml" >&2 2>/dev/null || true
  exit 1
fi
cp "$EVIDENCE_ROOT/window.xml" "$EVIDENCE_ROOT/window-recording.xml"
adb shell cmd appops get "$APP_ID" > "$EVIDENCE_ROOT/appops-during-recording.txt" 2>/dev/null || true
sleep 4

current_window
read -r stop_x stop_y < <(
  python3 - "$EVIDENCE_ROOT/window.xml" "$STOP_CONTENT_DESCRIPTION" "$APP_ID" <<'PY'
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
        match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            raise SystemExit(0)
raise SystemExit("Stop-video control was not found while recording")
PY
)
printf 'x=%s\ny=%s\n' "$stop_x" "$stop_y" > "$EVIDENCE_ROOT/stop-video-tap.txt"
adb shell input tap "$stop_x" "$stop_y"

for attempt in $(seq 1 45); do
  dismiss_external_system_anr_dialog
  current_window
  video_name="$(
    python3 - "$EVIDENCE_ROOT/window.xml" <<'PY' 2>/dev/null || true
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
prefix = "Video saved: "
for node in root.iter("node"):
    text = node.attrib.get("text", "")
    if text.startswith(prefix):
        print(text[len(prefix):])
        break
PY
  )"
  if [[ "$video_name" == GCAM_*.mp4 ]]; then
    video_saved=1
  fi
  if [ "$video_saved" -eq 1 ] && grep -q 'text="Session: previewing' "$EVIDENCE_ROOT/window.xml" 2>/dev/null; then
    preview_restored=1
    break
  fi
  sleep 1
done

if [ "$video_saved" -ne 1 ]; then
  echo "Camera did not report a saved MP4 recording." >&2
  cat "$EVIDENCE_ROOT/window.xml" >&2 2>/dev/null || true
  exit 1
fi
if [ "$preview_restored" -ne 1 ]; then
  echo "Camera preview did not restore after video finalization." >&2
  exit 1
fi

adb shell content query \
  --uri "$VIDEO_URI" \
  --projection _id:_display_name:mime_type:relative_path:_size \
  > "$EVIDENCE_ROOT/video-media-store.txt"

media_id="$(
  python3 - "$EVIDENCE_ROOT/video-media-store.txt" "$video_name" <<'PY'
import re
import sys

text = open(sys.argv[1], encoding="utf-8").read()
name = sys.argv[2]
for line in text.splitlines():
    if f"_display_name={name}" not in line:
        continue
    if "mime_type=video/mp4" not in line:
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
  echo "Published MediaStore MP4 row was not found or had zero size." >&2
  cat "$EVIDENCE_ROOT/video-media-store.txt" >&2
  exit 1
fi
media_verified=1

adb exec-out content read --uri "${VIDEO_URI}/${media_id}" > "$EVIDENCE_ROOT/captured.mp4"
test -s "$EVIDENCE_ROOT/captured.mp4"

ffprobe -v error \
  -show_entries stream=index,codec_type,codec_name:format=duration,size \
  -of json \
  "$EVIDENCE_ROOT/captured.mp4" > "$EVIDENCE_ROOT/ffprobe.json"

read -r video_track_verified audio_track_verified < <(
  python3 - "$EVIDENCE_ROOT/ffprobe.json" <<'PY'
import json
import sys

payload = json.load(open(sys.argv[1], encoding="utf-8"))
streams = payload.get("streams", [])
video_ok = any(s.get("codec_type") == "video" and s.get("codec_name") == "h264" for s in streams)
audio_ok = any(s.get("codec_type") == "audio" and s.get("codec_name") == "aac" for s in streams)
try:
    duration = float(payload.get("format", {}).get("duration", "0"))
except (TypeError, ValueError):
    duration = 0.0
if duration <= 0.5:
    raise SystemExit(f"Recorded MP4 duration is too short: {duration}")
print(int(video_ok), int(audio_ok))
PY
)

if [ "$video_track_verified" -ne 1 ]; then
  echo "Recorded MP4 does not contain the expected H.264 video track." >&2
  cat "$EVIDENCE_ROOT/ffprobe.json" >&2
  exit 1
fi
if [ "$audio_track_verified" -ne 1 ]; then
  echo "Recorded MP4 does not contain the expected AAC audio track." >&2
  cat "$EVIDENCE_ROOT/ffprobe.json" >&2
  exit 1
fi

echo "Camera video/audio runtime reached an explicit-permission recording, published a non-empty H.264/AAC MP4, and restored preview on the Android 16 representative emulator."
