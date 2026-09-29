#!/usr/bin/env python3
# File internal version: 0.4.2
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
APP_GRADLE = ROOT / "app" / "build.gradle.kts"
MANIFEST = ROOT / "app" / "src" / "main" / "AndroidManifest.xml"
MAIN_ACTIVITY = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "goreecloud" / "camera" / "MainActivity.kt"
CAMERA_ROOT = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "goreecloud" / "camera"
CONTROLLER = CAMERA_ROOT / "camera" / "CameraSessionController.kt"
PHOTO_COMMITTER = CAMERA_ROOT / "storage" / "PhotoMediaStoreCommitter.kt"
VIDEO_NAMER = CAMERA_ROOT / "storage" / "VideoFileNamer.kt"
VIDEO_COMMITTER = CAMERA_ROOT / "storage" / "VideoMediaStoreCommitter.kt"
SETTINGS_STORE = CAMERA_ROOT / "settings" / "CameraSettingsStore.kt"
COMPOSITION_GRID = CAMERA_ROOT / "ui" / "CompositionGridView.kt"

EXPECTED = {
    "applicationId": "com.goreecloud.camera",
    "namespace": "com.goreecloud.camera",
    "compileSdk": "37",
    "minSdk": "29",
    "targetSdk": "37",
    "versionName": "0.1.0",
}

REQUIRED_FILES = [
    ROOT / "settings.gradle.kts",
    ROOT / "build.gradle.kts",
    ROOT / "gradle.properties",
    APP_GRADLE,
    MANIFEST,
    MAIN_ACTIVITY,
    CAMERA_ROOT / "camera" / "CameraCapabilityRegistry.kt",
    CONTROLLER,
    CAMERA_ROOT / "storage" / "PhotoFileNamer.kt",
    PHOTO_COMMITTER,
    VIDEO_NAMER,
    VIDEO_COMMITTER,
    SETTINGS_STORE,
    COMPOSITION_GRID,
]

REQUIRED_PERMISSIONS = {
    "android.permission.CAMERA",
    "android.permission.RECORD_AUDIO",
}

FORBIDDEN_PERMISSIONS = {
    "android.permission.INTERNET",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.ACCESS_MEDIA_LOCATION",
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    "android.permission.READ_MEDIA_IMAGES",
    "android.permission.READ_MEDIA_VIDEO",
    "android.permission.READ_MEDIA_AUDIO",
}

ANDROID_NS = "{http://schemas.android.com/apk/res/android}"


def fail(message: str) -> None:
    print(f"camera-foundation: {message}", file=sys.stderr)
    raise SystemExit(1)


for path in REQUIRED_FILES:
    if not path.is_file():
        fail(f"missing required file: {path.relative_to(ROOT)}")

build_text = APP_GRADLE.read_text(encoding="utf-8")
for key, expected in EXPECTED.items():
    if key in {"compileSdk", "minSdk", "targetSdk"}:
        pattern = rf"\b{re.escape(key)}\s*=\s*{re.escape(expected)}\b"
    else:
        pattern = rf'\b{re.escape(key)}\s*=\s*"{re.escape(expected)}"'
    if not re.search(pattern, build_text):
        fail(f"{key} is not pinned to {expected}")

if 'id("com.android.application") version "9.4.0"' not in (ROOT / "build.gradle.kts").read_text(encoding="utf-8"):
    fail("Android Gradle plugin is not pinned to 9.4.0")

manifest_root = ET.parse(MANIFEST).getroot()
permissions = {
    element.attrib.get(ANDROID_NS + "name")
    for element in manifest_root.findall("uses-permission")
}
if permissions != REQUIRED_PERMISSIONS:
    fail(
        "runtime permissions must remain camera + just-in-time microphone only, "
        f"found: {sorted(permissions)}"
    )
for forbidden in sorted(FORBIDDEN_PERMISSIONS):
    if forbidden in permissions:
        fail(f"unexpected sensitive permission in capture milestone: {forbidden}")

feature_names = {
    element.attrib.get(ANDROID_NS + "name")
    for element in manifest_root.findall("uses-feature")
}
if "android.hardware.microphone" not in feature_names:
    fail("optional microphone hardware feature declaration is missing")

activity_text = MAIN_ACTIVITY.read_text(encoding="utf-8")
for required_fragment in (
    "setOnApplyWindowInsetsListener",
    "WindowInsets.Type.systemBars()",
    "systemWindowInsetBottom",
    "capturePaddingBottom + bottomSystemInset",
    "REQUEST_RECORD_AUDIO_PERMISSION",
    "requestPermissions(\n                arrayOf(Manifest.permission.RECORD_AUDIO)",
    "R.string.video_recording",
    "sessionController.startVideoRecording()",
    "REQUEST_RECORD_AUDIO_PERMISSION ->",
):
    if required_fragment not in activity_text:
        fail(f"capture UI/permission contract is missing: {required_fragment}")

on_create_prefix = activity_text.split("override fun onCreate", 1)[0]
if "requestPermissions" in on_create_prefix:
    fail("runtime permissions must not be requested before explicit user action")

settings_text = SETTINGS_STORE.read_text(encoding="utf-8")
for required_fragment in (
    'PREFERENCES_NAME = "goreecloud_camera_settings"',
    'KEY_COMPOSITION_GRID_ENABLED = "composition_grid_enabled"',
    'getBoolean(KEY_COMPOSITION_GRID_ENABLED, false)',
    'putBoolean(KEY_COMPOSITION_GRID_ENABLED, enabled)',
):
    if required_fragment not in settings_text:
        fail(f"local Camera settings contract is missing: {required_fragment}")

grid_text = COMPOSITION_GRID.read_text(encoding="utf-8")
for required_fragment in (
    "class CompositionGridView",
    "IMPORTANT_FOR_ACCESSIBILITY_NO",
    "isClickable = false",
    "isFocusable = false",
    "canvas.drawLine",
):
    if required_fragment not in grid_text:
        fail(f"composition grid source contract is missing: {required_fragment}")

controller_text = CONTROLLER.read_text(encoding="utf-8")
for required_fragment in (
    "CameraDevice.TEMPLATE_STILL_CAPTURE",
    "ImageReader.newInstance",
    "ImageFormat.JPEG",
    "CameraDevice.TEMPLATE_RECORD",
    "MediaRecorder.AudioSource.MIC",
    "MediaRecorder.AudioEncoder.AAC",
    "MediaRecorder.VideoEncoder.H264",
    "MediaRecorder.OutputFormat.MPEG_4",
    "Manifest.permission.RECORD_AUDIO",
    "PackageManager.FEATURE_MICROPHONE",
    "videoCommitter.publish",
    "videoCommitter.discard",
):
    if required_fragment not in controller_text:
        fail(f"camera/video source contract is missing: {required_fragment}")

photo_committer_text = PHOTO_COMMITTER.read_text(encoding="utf-8")
for required_fragment in (
    "MediaStore.Images.Media.IS_PENDING",
    "MediaStore.Images.Media.RELATIVE_PATH",
    "MediaStore.VOLUME_EXTERNAL_PRIMARY",
):
    if required_fragment not in photo_committer_text:
        fail(f"MediaStore photo contract is missing: {required_fragment}")

video_committer_text = VIDEO_COMMITTER.read_text(encoding="utf-8")
for required_fragment in (
    "MediaStore.Video.Media.IS_PENDING",
    "MediaStore.Video.Media.RELATIVE_PATH",
    "MediaStore.VOLUME_EXTERNAL_PRIMARY",
    "android.system.Os",
    "Os.fstat(pendingVideo.fileDescriptor.fileDescriptor).st_size",
):
    if required_fragment not in video_committer_text:
        fail(f"MediaStore video contract is missing: {required_fragment}")
if "MediaStore.Video.Media.SIZE" in video_committer_text:
    fail("pending video publication must validate the open descriptor, not stale MediaStore SIZE metadata")

video_namer_text = VIDEO_NAMER.read_text(encoding="utf-8")
if '"GCAM_${formatter.format(Instant.ofEpochMilli(epochMillis))}.mp4"' not in video_namer_text:
    fail("deterministic UTC MP4 naming contract is missing")

platform_path = ROOT / "goreecloud.platform.yaml"
platform_text = platform_path.read_text(encoding="utf-8") if platform_path.exists() else ""
if "lifecycle: concept" not in platform_text:
    fail("Platform Contract lifecycle must remain concept")
if 'version: "0.1.0"' not in platform_text:
    fail("Platform Contract product version must remain 0.1.0")

print("camera-foundation: preview, JPEG, and bounded video/audio source contract checks passed")
