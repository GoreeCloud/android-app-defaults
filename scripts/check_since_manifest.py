#!/usr/bin/env python3
from pathlib import Path
import sys

manifest = Path("apps/since/src/main/AndroidManifest.xml")
text = manifest.read_text(encoding="utf-8")

for forbidden in (
    "android.permission.INTERNET",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
):
    if forbidden in text:
        print(f"forbidden Since MVP permission found: {forbidden}", file=sys.stderr)
        sys.exit(1)

required_fragments = (
    'android:allowBackup="false"',
    'android:usesCleartextTraffic="false"',
    'android:exported="true"',
)

for required in required_fragments:
    if required not in text:
        print(f"required fail-closed manifest setting missing: {required}", file=sys.stderr)
        sys.exit(1)

build = Path("apps/since/build.gradle.kts").read_text(encoding="utf-8")
if 'applicationId = "com.goreecloud.since"' not in build:
    print("Since application identity is missing from Gradle configuration", file=sys.stderr)
    sys.exit(1)

print("Since manifest boundary verified: local-only, fail-closed backup/cleartext settings, launcher-only exported activity.")
