#!/usr/bin/env python3
"""Apply GoreeCloud Gallery gc.14 Glaze UI media-viewer integration."""
from __future__ import annotations

import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ANDROID_NS = "http://schemas.android.com/apk/res/android"
ET.register_namespace("android", ANDROID_NS)
ET.register_namespace("app", "http://schemas.android.com/apk/res-auto")

GALLERY_COMMIT = "b28299dc33821eee8d108a9880ce87876cf31443"
COMMONS_COMMIT = "acfd352df1a1852d17a5f77def8b7ad6e522a5b6"
VERSION_NAME = "1.0.0"
VERSION_CODE = "10014"
GLAZE_UI_VERSION = "1.0.0"
GLAZE_UI_REFERENCE = "d6e446fd8ef251259d16368d50aad90d9287a774"


def fail(message: str) -> None:
    raise SystemExit(f"gc.14 patch failed: {message}")


def read(path: Path) -> str:
    if not path.is_file():
        fail(f"missing {path}")
    return path.read_text(encoding="utf-8")


def write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def verify(root: Path, expected: str, label: str) -> None:
    actual = subprocess.check_output(["git", "-C", str(root), "rev-parse", "HEAD"], text=True).strip()
    if actual != expected:
        fail(f"{label} checkout is {actual}, expected {expected}")


def update_version(gallery: Path) -> None:
    path = gallery / "gradle.properties"
    content = read(path)
    content, n1 = re.subn(r"^VERSION_NAME=.*$", f"VERSION_NAME={VERSION_NAME}", content, count=1, flags=re.M)
    content, n2 = re.subn(r"^VERSION_CODE=.*$", f"VERSION_CODE={VERSION_CODE}", content, count=1, flags=re.M)
    if n1 != 1 or n2 != 1:
        fail("could not update version metadata")
    write(path, content)


def write_viewer_overlay(gallery: Path) -> None:
    drawable = gallery / "app/src/main/res/drawable/goreecloud_glaze_viewer_actions.xml"
    write(drawable, """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/goreecloud_glaze_surface_muted" />
    <stroke android:width="1dp" android:color="@color/goreecloud_glaze_line" />
    <corners
        android:topLeftRadius="@dimen/goreecloud_glaze_radius_lg"
        android:topRightRadius="@dimen/goreecloud_glaze_radius_lg" />
</shape>
""")


def patch_medium(gallery: Path) -> None:
    path = gallery / "app/src/main/res/layout/activity_medium.xml"
    tree = ET.parse(path)
    root = tree.getroot()
    aid = f"{{{ANDROID_NS}}}id"
    abg = f"{{{ANDROID_NS}}}background"
    amin = f"{{{ANDROID_NS}}}minHeight"
    toolbar = False
    for element in root.iter():
        view_id = element.attrib.get(aid, "")
        if view_id == "@+id/medium_viewer_toolbar":
            element.set(abg, "@color/goreecloud_glaze_surface_muted")
            element.set(amin, "@dimen/goreecloud_glaze_target_comfortable")
            toolbar = True
    if not toolbar:
        fail("media viewer toolbar not found")
    ET.indent(tree, space="    ")
    tree.write(path, encoding="unicode", xml_declaration=True)


def patch_bottom_actions(gallery: Path) -> None:
    path = gallery / "app/src/main/res/layout/bottom_actions.xml"
    tree = ET.parse(path)
    root = tree.getroot()
    aid = f"{{{ANDROID_NS}}}id"
    abg = f"{{{ANDROID_NS}}}background"
    aw = f"{{{ANDROID_NS}}}layout_width"
    ah = f"{{{ANDROID_NS}}}layout_height"
    amin = f"{{{ANDROID_NS}}}minHeight"
    root.set(abg, "@drawable/goreecloud_glaze_viewer_actions")
    root.set(amin, "@dimen/goreecloud_glaze_target_comfortable")
    count = 0
    for element in root.iter():
        view_id = element.attrib.get(aid, "")
        if view_id.startswith("@+id/bottom_") and view_id != "@+id/bottom_actions_wrapper":
            element.set(aw, "@dimen/goreecloud_glaze_target_comfortable")
            element.set(ah, "@dimen/goreecloud_glaze_target_comfortable")
            count += 1
    if count < 8:
        fail(f"unexpected media viewer action count: {count}")
    ET.indent(tree, space="    ")
    tree.write(path, encoding="unicode", xml_declaration=True)


def update_notice(gallery: Path) -> None:
    path = gallery / "GOREECLOUD-NOTICE.md"
    notice = read(path).rstrip() + f"""

gc.14 extends the native Glaze UI {GLAZE_UI_VERSION} mapping to the full-screen media viewer using canonical reference revision {GLAZE_UI_REFERENCE}. Viewer chrome now uses a restrained muted Glaze overlay rather than decorative card treatment, while primary bottom actions use 48dp comfortable targets. Media remains the dominant visual layer and destructive-action behavior is not changed.
"""
    write(path, notice)
    write(gallery / "app/src/main/assets/goreecloud_notice.txt", notice)


def validate(gallery: Path) -> None:
    medium = read(gallery / "app/src/main/res/layout/activity_medium.xml")
    bottom = read(gallery / "app/src/main/res/layout/bottom_actions.xml")
    overlay = read(gallery / "app/src/main/res/drawable/goreecloud_glaze_viewer_actions.xml")
    props = read(gallery / "gradle.properties")
    for required in ("@color/goreecloud_glaze_surface_muted", "@dimen/goreecloud_glaze_target_comfortable"):
        if required not in medium:
            fail(f"missing viewer toolbar invariant: {required}")
    if "@drawable/goreecloud_glaze_viewer_actions" not in bottom:
        fail("missing Glaze viewer action overlay")
    if bottom.count("@dimen/goreecloud_glaze_target_comfortable") < 8:
        fail("comfortable viewer action targets are missing")
    if "@color/goreecloud_glaze_surface_muted" not in overlay or "@color/goreecloud_glaze_line" not in overlay:
        fail("viewer overlay semantic resources are missing")
    for required in (f"VERSION_NAME={VERSION_NAME}", f"VERSION_CODE={VERSION_CODE}"):
        if required not in props:
            fail(f"missing version invariant: {required}")


def main() -> None:
    if len(sys.argv) != 3:
        fail("usage: build_goreecloud_gallery_gc14.py <gallery-root> <commons-root>")
    gallery = Path(sys.argv[1]).resolve()
    commons = Path(sys.argv[2]).resolve()
    verify(gallery, GALLERY_COMMIT, "Gallery")
    verify(commons, COMMONS_COMMIT, "Commons")
    update_version(gallery)
    write_viewer_overlay(gallery)
    patch_medium(gallery)
    patch_bottom_actions(gallery)
    update_notice(gallery)
    validate(gallery)
    print(f"Applied GoreeCloud Gallery gc.14 Glaze media-viewer integration: {VERSION_NAME} ({VERSION_CODE}).")


if __name__ == "__main__":
    main()
