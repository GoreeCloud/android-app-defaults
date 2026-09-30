#!/usr/bin/env python3
"""Apply GoreeCloud Gallery gc.16 dialog geometry refinement."""
from __future__ import annotations

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
VERSION_CODE = "10016"


def fail(message: str) -> None:
    raise SystemExit(f"gc.16 patch failed: {message}")


def read(path: Path) -> str:
    if not path.is_file():
        fail(f"missing {path}")
    return path.read_text(encoding="utf-8")


def write(path: Path, content: str) -> None:
    path.write_text(content, encoding="utf-8")


def verify(root: Path, expected: str, label: str) -> None:
    actual = subprocess.check_output(["git", "-C", str(root), "rev-parse", "HEAD"], text=True).strip()
    if actual != expected:
        fail(f"{label} checkout is {actual}, expected {expected}")


def update_version(gallery: Path) -> None:
    path = gallery / "gradle.properties"
    content = read(path).replace("VERSION_CODE=10015", f"VERSION_CODE={VERSION_CODE}", 1)
    if f"VERSION_CODE={VERSION_CODE}" not in content:
        fail("could not update versionCode")
    write(path, content)


def compact_dialog(path: Path) -> None:
    tree = ET.parse(path)
    root = tree.getroot()
    height = f"{{{ANDROID_NS}}}layout_height"
    width = f"{{{ANDROID_NS}}}layout_width"
    fill = f"{{{ANDROID_NS}}}fillViewport"
    fading = f"{{{ANDROID_NS}}}fadingEdge"
    over_scroll = f"{{{ANDROID_NS}}}overScrollMode"

    root.set(width, "match_parent")
    root.set(height, "wrap_content")
    root.set(fill, "false")
    root.set(fading, "none")
    root.set(over_scroll, "ifContentScrolls")

    ET.indent(tree, space="    ")
    tree.write(path, encoding="unicode", xml_declaration=True)


def update_notice(gallery: Path) -> None:
    path = gallery / "GOREECLOUD-NOTICE.md"
    notice = read(path).rstrip() + """

gc.16 refines the gc.15 Glaze transient-surface implementation after representative-device review. Sorting, grouping, and media-filter ScrollViews now measure to their content instead of claiming the full dialog height, eliminating excessive empty dialog space and keeping the rounded Glaze surface visually attached to its controls. The change is presentation-only and preserves sorting, grouping, filtering, confirmation, permissions, file operations, privacy, and offline behavior.
"""
    write(path, notice)
    write(gallery / "app/src/main/assets/goreecloud_notice.txt", notice)


def validate(gallery: Path) -> None:
    props = read(gallery / "gradle.properties")
    if f"VERSION_NAME={VERSION_NAME}" not in props or f"VERSION_CODE={VERSION_CODE}" not in props:
        fail("version identity mismatch")
    for name in ("dialog_change_sorting.xml", "dialog_change_grouping.xml", "dialog_filter_media.xml"):
        content = read(gallery / "app/src/main/res/layout" / name)
        for required in (
            'android:layout_height="wrap_content"',
            'android:fillViewport="false"',
            'android:overScrollMode="ifContentScrolls"',
            '@drawable/goreecloud_glaze_dialog_surface',
        ):
            if required not in content:
                fail(f"{name} missing compact Glaze dialog invariant: {required}")
    if "gc.16 refines the gc.15 Glaze transient-surface implementation" not in read(gallery / "GOREECLOUD-NOTICE.md"):
        fail("gc.16 notice missing")


def main() -> None:
    if len(sys.argv) != 3:
        fail("usage: build_goreecloud_gallery_gc16.py <gallery-root> <commons-root>")
    gallery = Path(sys.argv[1]).resolve()
    commons = Path(sys.argv[2]).resolve()
    verify(gallery, GALLERY_COMMIT, "Gallery")
    verify(commons, COMMONS_COMMIT, "Commons")
    update_version(gallery)
    for name in ("dialog_change_sorting.xml", "dialog_change_grouping.xml", "dialog_filter_media.xml"):
        compact_dialog(gallery / "app/src/main/res/layout" / name)
    update_notice(gallery)
    validate(gallery)
    print(f"Applied GoreeCloud Gallery gc.16 compact Glaze dialog geometry: {VERSION_NAME} ({VERSION_CODE}).")


if __name__ == "__main__":
    main()
