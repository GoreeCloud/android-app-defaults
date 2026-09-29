#!/usr/bin/env python3
"""Apply GoreeCloud Gallery gc.13 Glaze UI search-surface integration."""
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
VERSION_CODE = "10013"
GLAZE_UI_VERSION = "1.0.0"
GLAZE_UI_REFERENCE = "d6e446fd8ef251259d16368d50aad90d9287a774"


def fail(message: str) -> None:
    raise SystemExit(f"gc.13 patch failed: {message}")


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
    content, name_count = re.subn(r"^VERSION_NAME=.*$", f"VERSION_NAME={VERSION_NAME}", content, count=1, flags=re.M)
    content, code_count = re.subn(r"^VERSION_CODE=.*$", f"VERSION_CODE={VERSION_CODE}", content, count=1, flags=re.M)
    if name_count != 1 or code_count != 1:
        fail("could not update version metadata")
    write(path, content)


def write_search_resource(gallery: Path) -> None:
    drawable = gallery / "app/src/main/res/drawable"
    write(drawable / "goreecloud_glaze_empty_state.xml", """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/goreecloud_glaze_surface_muted" />
    <stroke android:width="1dp" android:color="@color/goreecloud_glaze_line" />
    <corners android:radius="@dimen/goreecloud_glaze_radius_lg" />
    <padding
        android:left="@dimen/goreecloud_glaze_space_4"
        android:top="@dimen/goreecloud_glaze_space_4"
        android:right="@dimen/goreecloud_glaze_space_4"
        android:bottom="@dimen/goreecloud_glaze_space_4" />
</shape>
""")


def patch_search_layout(gallery: Path) -> None:
    path = gallery / "app/src/main/res/layout/activity_search.xml"
    tree = ET.parse(path)
    root = tree.getroot()

    aid = f"{{{ANDROID_NS}}}id"
    abg = f"{{{ANDROID_NS}}}background"
    amin = f"{{{ANDROID_NS}}}minHeight"
    apad_start = f"{{{ANDROID_NS}}}paddingStart"
    apad_end = f"{{{ANDROID_NS}}}paddingEnd"
    apad_bottom = f"{{{ANDROID_NS}}}paddingBottom"
    amargin_start = f"{{{ANDROID_NS}}}layout_marginStart"
    amargin_end = f"{{{ANDROID_NS}}}layout_marginEnd"
    amargin_top = f"{{{ANDROID_NS}}}layout_marginTop"
    aclip = f"{{{ANDROID_NS}}}clipToPadding"

    root.set(abg, "@drawable/goreecloud_glaze_canvas")
    found = {"menu": False, "holder": False, "grid": False, "empty": False}

    for element in root.iter():
        view_id = element.attrib.get(aid, "")
        if view_id == "@+id/search_menu":
            element.set(abg, "@drawable/goreecloud_glaze_toolbar")
            element.set(amin, "@dimen/goreecloud_glaze_target_comfortable")
            found["menu"] = True
        elif view_id == "@+id/search_holder":
            element.set(apad_start, "@dimen/goreecloud_glaze_browser_horizontal_inset")
            element.set(apad_end, "@dimen/goreecloud_glaze_browser_horizontal_inset")
            element.set(aclip, "false")
            found["holder"] = True
        elif view_id == "@+id/search_grid":
            element.set(apad_bottom, "@dimen/goreecloud_glaze_browser_vertical_inset")
            element.set(aclip, "false")
            found["grid"] = True
        elif view_id == "@+id/search_empty_text_placeholder":
            element.set(abg, "@drawable/goreecloud_glaze_empty_state")
            element.set(amargin_start, "@dimen/goreecloud_glaze_space_4")
            element.set(amargin_end, "@dimen/goreecloud_glaze_space_4")
            element.set(amargin_top, "@dimen/goreecloud_glaze_space_4")
            found["empty"] = True

    missing = [key for key, present in found.items() if not present]
    if missing:
        fail(f"search Glaze mapping missing: {', '.join(missing)}")

    ET.indent(tree, space="    ")
    tree.write(path, encoding="unicode", xml_declaration=True)


def update_notice(gallery: Path) -> None:
    notice_path = gallery / "GOREECLOUD-NOTICE.md"
    notice = read(notice_path).rstrip() + f"""

gc.13 extends the native Glaze UI {GLAZE_UI_VERSION} mapping to Gallery search using canonical reference revision {GLAZE_UI_REFERENCE}. Search now shares the Glaze Canvas, GoreeCloud accent-gradient menu chrome, adaptive media-aware gutters, and a rounded muted Raised empty-state surface while keeping result thumbnails visually dominant.

The search treatment deliberately reuses the same compact browsing inset contract established by gc.12, so folder browsing, opened-folder media, and global search form one coherent Android-native visual family without introducing remote UI assets, analytics, tracking, advertising, or an Internet requirement.
"""
    write(notice_path, notice)
    write(gallery / "app/src/main/assets/goreecloud_notice.txt", notice)


def validate(gallery: Path) -> None:
    layout = read(gallery / "app/src/main/res/layout/activity_search.xml")
    empty = read(gallery / "app/src/main/res/drawable/goreecloud_glaze_empty_state.xml")
    properties = read(gallery / "gradle.properties")

    for value in (
        "@drawable/goreecloud_glaze_canvas",
        "@drawable/goreecloud_glaze_toolbar",
        "@dimen/goreecloud_glaze_browser_horizontal_inset",
        "@dimen/goreecloud_glaze_browser_vertical_inset",
        "@drawable/goreecloud_glaze_empty_state",
        "@dimen/goreecloud_glaze_target_comfortable",
    ):
        if value not in layout:
            fail(f"missing search invariant: {value}")

    if "@color/goreecloud_glaze_surface_muted" not in empty or "@color/goreecloud_glaze_line" not in empty:
        fail("missing Glaze empty-state surface styling")

    for required in (f"VERSION_NAME={VERSION_NAME}", f"VERSION_CODE={VERSION_CODE}"):
        if required not in properties:
            fail(f"missing version invariant: {required}")


def main() -> None:
    if len(sys.argv) != 3:
        fail("usage: build_goreecloud_gallery_gc13.py <gallery-root> <commons-root>")
    gallery = Path(sys.argv[1]).resolve()
    commons = Path(sys.argv[2]).resolve()
    verify(gallery, GALLERY_COMMIT, "Gallery")
    verify(commons, COMMONS_COMMIT, "Commons")
    update_version(gallery)
    write_search_resource(gallery)
    patch_search_layout(gallery)
    update_notice(gallery)
    validate(gallery)
    print(f"Applied GoreeCloud Gallery gc.13 Glaze search integration: {VERSION_NAME} ({VERSION_CODE}).")


if __name__ == "__main__":
    main()
