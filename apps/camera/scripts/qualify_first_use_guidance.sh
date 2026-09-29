#!/usr/bin/env bash
# File internal version: 0.1.0
set -euo pipefail

readonly APP_ID="${1:-com.goreecloud.camera}"
readonly ACTIVITY="${APP_ID}/.MainActivity"
readonly WINDOW_XML="/tmp/goreecloud-camera-guidance-window.xml"

dump_window() {
  local attempt
  for attempt in 1 2; do
    adb shell uiautomator dump /sdcard/goreecloud-camera-guidance.xml >/dev/null 2>&1
    adb pull /sdcard/goreecloud-camera-guidance.xml "$WINDOW_XML" >/dev/null 2>&1

    local quickstep_wait
    quickstep_wait="$(
      python3 - "$WINDOW_XML" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
texts = {node.attrib.get("text", "") for node in root.iter("node")}
if "Quickstep isn't responding" not in texts:
    raise SystemExit(0)
for node in root.iter("node"):
    if node.attrib.get("package") == "android" and node.attrib.get("text") == "Wait":
        match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            break
PY
    )"
    if [ -z "$quickstep_wait" ]; then
      return
    fi

    read -r wait_x wait_y <<<"$quickstep_wait"
    adb shell input tap "$wait_x" "$wait_y"
    sleep 2
  done

  adb shell uiautomator dump /sdcard/goreecloud-camera-guidance.xml >/dev/null 2>&1
  adb pull /sdcard/goreecloud-camera-guidance.xml "$WINDOW_XML" >/dev/null 2>&1
}

assert_text() {
  local expected="$1"
  dump_window
  python3 - "$WINDOW_XML" "$expected" <<'PY'
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
expected = sys.argv[2]
if not any(node.attrib.get("text") == expected for node in root.iter("node")):
    raise SystemExit(f"Expected Camera guidance text not found: {expected}")
PY
}


assert_desc() {
  local expected="$1"
  dump_window
  python3 - "$WINDOW_XML" "$expected" <<'PY'
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
expected = sys.argv[2]
if not any(node.attrib.get("content-desc") == expected for node in root.iter("node")):
    raise SystemExit(f"Expected Camera content description not found: {expected}")
PY
}

assert_desc_absent() {
  local unexpected="$1"
  dump_window
  python3 - "$WINDOW_XML" "$unexpected" <<'PY'
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
unexpected = sys.argv[2]
if any(node.attrib.get("content-desc") == unexpected for node in root.iter("node")):
    raise SystemExit(f"Unexpected Camera content description remained visible: {unexpected}")
PY
}

assert_text_absent() {
  local unexpected="$1"
  dump_window
  python3 - "$WINDOW_XML" "$unexpected" <<'PY'
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
unexpected = sys.argv[2]
if any(node.attrib.get("text") == unexpected for node in root.iter("node")):
    raise SystemExit(f"Unexpected Camera guidance text remained visible: {unexpected}")
PY
}

assert_no_tip() {
  dump_window
  python3 - "$WINDOW_XML" <<'PY'
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
tips = [
    node.attrib.get("text", "")
    for node in root.iter("node")
    if node.attrib.get("package") == "com.goreecloud.camera"
    and node.attrib.get("text", "").startswith("Tip:")
]
if tips:
    raise SystemExit(f"Contextual hints should be disabled, found: {tips}")
PY
}

tap_text() {
  local expected="$1"
  dump_window
  local coordinates
  coordinates="$(
    python3 - "$WINDOW_XML" "$expected" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
expected = sys.argv[2]
for node in root.iter("node"):
    if (
        node.attrib.get("package") == "com.goreecloud.camera"
        and node.attrib.get("text") == expected
        and node.attrib.get("clickable") == "true"
        and node.attrib.get("enabled") == "true"
    ):
        match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            raise SystemExit(0)
raise SystemExit(f"Clickable Camera guidance control not found: {expected}")
PY
  )"
  read -r tap_x tap_y <<<"$coordinates"
  adb shell input tap "$tap_x" "$tap_y"
  sleep 1
}

restart_app() {
  adb shell am force-stop "$APP_ID"
  adb shell am start -W -n "$ACTIVITY" >/dev/null
  sleep 1
}

# First use is mandatory and begins at step 1.
assert_text "Welcome to GoreeCloud Camera"
assert_text "Step 1 of 3"
assert_text "Camera preview and capture use this device. Volume keys capture photos by default; change that behavior, the photo self-timer, or the composition grid in Settings. This Development build does not upload captures or require a cloud account to take a photo or video."
tap_text "Next"

# Interruption/resume must persist the exact incomplete step.
restart_app
assert_text "Welcome to GoreeCloud Camera"
assert_text "Step 2 of 3"
tap_text "Next"
assert_text "Step 3 of 3"
tap_text "Finish"

# Completion survives process restart and exposes the replay/help entry.
restart_app
assert_text_absent "Welcome to GoreeCloud Camera"
assert_text "Help & guidance"

# Local Camera settings are presentation-only and persist across restart.
tap_text "Settings"
assert_text "Camera settings"
assert_text "Composition grid: Off"
assert_text "Volume keys capture photo: On"
tap_text "Composition grid: Off"
assert_text "Composition grid: On"
tap_text "Volume keys capture photo: On"
assert_text "Volume keys capture photo: Off"
tap_text "Close"
assert_desc_absent "Composition grid overlay"

restart_app
assert_text_absent "Welcome to GoreeCloud Camera"
assert_desc_absent "Composition grid overlay"
tap_text "Settings"
assert_text "Composition grid: On"
assert_text "Volume keys capture photo: Off"
tap_text "Composition grid: On"
assert_text "Composition grid: Off"
tap_text "Volume keys capture photo: Off"
assert_text "Volume keys capture photo: On"
tap_text "Close"
assert_desc_absent "Composition grid overlay"

# Contextual hints can be disabled globally and the preference survives restart.
tap_text "Help & guidance"
assert_text "Camera guidance"
tap_text "Turn contextual hints off"
tap_text "Close"
restart_app
assert_no_tip

# Re-enable hints, then prove voluntary replay does not reset first-use completion.
tap_text "Help & guidance"
tap_text "Turn contextual hints on"
tap_text "Replay startup guide"
assert_text "Step 1 of 3"
tap_text "Close"
restart_app
assert_text_absent "Welcome to GoreeCloud Camera"
assert_text "Help & guidance"

echo "Camera first-use resume, completion, replay, contextual-hint controls, composition grid, and volume-key shutter settings verified."
