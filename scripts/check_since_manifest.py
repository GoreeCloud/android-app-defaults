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

if "com.goreecloud.since" not in Path("apps/since/build.gradle.kts").read_text(encoding="utf-8"):
    print("Since application identity is missing from Gradle configuration", file=sys.stderr)
    sys.exit(1)

print("Since manifest boundary verified: no network or location permission declarations.")
