#!/usr/bin/env python3
"""Apply GoreeCloud Gallery gc.12 Glaze UI browsing-surface integration."""
from __future__ import annotations

import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ANDROID_NS = "http://schemas.android.com/apk/res/android"
APP_NS = "http://schemas.android.com/apk/res-auto"
ET.register_namespace("android", ANDROID_NS)
ET.register_namespace("app", APP_NS)
ET.register_namespace("tools", "http://schemas.android.com/tools")

GALLERY_COMMIT = "b28299dc33821eee8d108a9880ce87876cf31443"
COMMONS_COMMIT = "acfd352df1a1852d17a5f77def8b7ad6e522a5b6"
VERSION_NAME = "1.0.0"
VERSION_CODE = "10012"
GLAZE_UI_VERSION = "1.0.0"
GLAZE_UI_REFERENCE = "d6e446fd8ef251259d16368d50aad90d9287a774"


def fail(message: str) -> None:
    raise SystemExit(f"gc.12 patch failed: {message}")


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


def ensure_dimen(path: Path, name: str, value: str) -> None:
    tree = ET.parse(path)
    root = tree.getroot()
    matches = [item for item in root.findall("dimen") if item.attrib.get("name") == name]
    if len(matches) > 1:
        fail(f"duplicate dimen {name} in {path}")
    if matches:
        matches[0].text = value
    else:
        item = ET.SubElement(root, "dimen", {"name": name})
        item.text = value
    ET.indent(tree, space="    ")
    tree.write(path, encoding="unicode", xml_declaration=True)


def write_browse_resources(gallery: Path) -> None:
    values = gallery / "app/src/main/res/values/goreecloud_glaze.xml"
    sw600 = gallery / "app/src/main/res/values-sw600dp/goreecloud_glaze.xml"
    sw840 = gallery / "app/src/main/res/values-sw840dp/goreecloud_glaze.xml"
    drawable = gallery / "app/src/main/res/drawable"

    ensure_dimen(values, "goreecloud_glaze_browser_horizontal_inset", "8dp")
    ensure_dimen(values, "goreecloud_glaze_browser_vertical_inset", "8dp")
    ensure_dimen(sw600, "goreecloud_glaze_browser_horizontal_inset", "16dp")
    ensure_dimen(sw840, "goreecloud_glaze_browser_horizontal_inset", "24dp")

    write(drawable / "goreecloud_glaze_empty_action.xml", """<?xml version="1.0" encoding="utf-8"?>
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="?attr/colorControlHighlight">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/goreecloud_glaze_surface" />
            <stroke android:width="1dp" android:color="@color/goreecloud_glaze_line" />
            <corners android:radius="@dimen/goreecloud_glaze_radius_control" />
            <padding
                android:left="@dimen/goreecloud_glaze_space_2"
                android:top="@dimen/goreecloud_glaze_space_1"
                android:right="@dimen/goreecloud_glaze_space_2"
                android:bottom="@dimen/goreecloud_glaze_space_1" />
        </shape>
    </item>
</ripple>
""")


def patch_layout(path: Path, *, mode: str) -> None:
    tree = ET.parse(path)
    root = tree.getroot()
    aid = f"{{{ANDROID_NS}}}id"
    abg = f"{{{ANDROID_NS}}}background"
    amin = f"{{{ANDROID_NS}}}minHeight"
    apad_start = f"{{{ANDROID_NS}}}paddingStart"
    apad_end = f"{{{ANDROID_NS}}}paddingEnd"
    apad_bottom = f"{{{ANDROID_NS}}}paddingBottom"
    aclip = f"{{{ANDROID_NS}}}clipToPadding"
    indicator = f"{{{APP_NS}}}indicatorColor"

    root.set(abg, "@drawable/goreecloud_glaze_canvas")

    if mode == "directories":
        menu_id = "@+id/main_menu"
        holder_id = "@+id/directories_holder"
        grid_id = "@+id/directories_grid"
        primary_action_id = "@+id/directories_switch_searching"
        empty_action_id = "@+id/directories_empty_placeholder_2"
    elif mode == "media":
        menu_id = "@+id/media_menu"
        holder_id = "@+id/media_holder"
        grid_id = "@+id/media_grid"
        primary_action_id = None
        empty_action_id = "@+id/media_empty_text_placeholder_2"
    else:
        fail(f"unknown layout mode: {mode}")

    found = {"menu": False, "holder": False, "grid": False, "empty": False}
    for element in root.iter():
        view_id = element.attrib.get(aid, "")
        if view_id == menu_id:
            element.set(abg, "@drawable/goreecloud_glaze_toolbar")
            element.set(amin, "@dimen/goreecloud_glaze_target_comfortable")
            found["menu"] = True
        elif view_id == holder_id:
            element.set(apad_start, "@dimen/goreecloud_glaze_browser_horizontal_inset")
            element.set(apad_end, "@dimen/goreecloud_glaze_browser_horizontal_inset")
            element.set(aclip, "false")
            found["holder"] = True
        elif view_id == grid_id:
            element.set(apad_bottom, "@dimen/goreecloud_glaze_browser_vertical_inset")
            element.set(aclip, "false")
            found["grid"] = True
        elif primary_action_id and view_id == primary_action_id:
            element.set(abg, "@drawable/goreecloud_glaze_empty_action")
            element.set(amin, "@dimen/goreecloud_glaze_target_comfortable")
        elif view_id == empty_action_id:
            element.set(abg, "@drawable/goreecloud_glaze_empty_action")
            element.set(amin, "@dimen/goreecloud_glaze_target_comfortable")
            found["empty"] = True
        elif mode == "media" and view_id == "@+id/loading_indicator":
            element.set(indicator, "@color/goreecloud_glaze_accent")

    missing = [key for key, present in found.items() if not present]
    if missing:
        fail(f"{mode} Glaze mapping missing: {', '.join(missing)}")

    ET.indent(tree, space="    ")
    tree.write(path, encoding="unicode", xml_declaration=True)


def update_notice(gallery: Path) -> None:
    notice_path = gallery / "GOREECLOUD-NOTICE.md"
    notice = read(notice_path).rstrip() + f"""

gc.12 extends the native Glaze UI {GLAZE_UI_VERSION} mapping from Settings onto Gallery's highest-frequency browsing surfaces, using canonical reference revision {GLAZE_UI_REFERENCE}. The folders screen and opened-folder media screen now use the Glaze Canvas, GoreeCloud accent-gradient menu chrome, adaptive compact gutters, comfortable action targets, rounded Raised empty-state actions, and accent loading feedback while keeping media thumbnails visually dominant.

Browsing gutters are 8dp by default, 16dp at Android sw600dp, and 24dp at sw840dp. This is intentionally subtler than the Settings composition because media grids remain the primary visual content. The work adds no remote UI assets, analytics, tracking, advertising, or Internet requirement.
"""
    write(notice_path, notice)
    write(gallery / "app/src/main/assets/goreecloud_notice.txt", notice)


def validate(gallery: Path) -> None:
    main_layout = read(gallery / "app/src/main/res/layout/activity_main.xml")
    media_layout = read(gallery / "app/src/main/res/layout/activity_media.xml")
    values = read(gallery / "app/src/main/res/values/goreecloud_glaze.xml")
    sw600 = read(gallery / "app/src/main/res/values-sw600dp/goreecloud_glaze.xml")
    sw840 = read(gallery / "app/src/main/res/values-sw840dp/goreecloud_glaze.xml")
    action = read(gallery / "app/src/main/res/drawable/goreecloud_glaze_empty_action.xml")
    properties = read(gallery / "gradle.properties")

    for layout, label in ((main_layout, "main"), (media_layout, "media")):
        for value in (
            "@drawable/goreecloud_glaze_canvas",
            "@drawable/goreecloud_glaze_toolbar",
            "@dimen/goreecloud_glaze_browser_horizontal_inset",
            "@dimen/goreecloud_glaze_browser_vertical_inset",
            "@drawable/goreecloud_glaze_empty_action",
            "@dimen/goreecloud_glaze_target_comfortable",
        ):
            if value not in layout:
                fail(f"missing {label} browsing invariant: {value}")

    if "@color/goreecloud_glaze_accent" not in media_layout:
        fail("missing Glaze loading accent")
    if "goreecloud_glaze_browser_horizontal_inset\">8dp" not in values:
        fail("missing compact browsing inset")
    if "goreecloud_glaze_browser_vertical_inset\">8dp" not in values:
        fail("missing browsing vertical inset")
    if ">16dp<" not in sw600 or ">24dp<" not in sw840:
        fail("missing adaptive browsing insets")
    if "@color/goreecloud_glaze_surface" not in action or "@color/goreecloud_glaze_line" not in action:
        fail("missing Raised empty-action styling")
    for required in (f"VERSION_NAME={VERSION_NAME}", f"VERSION_CODE={VERSION_CODE}"):
        if required not in properties:
            fail(f"missing version invariant: {required}")


def main() -> None:
    if len(sys.argv) != 3:
        fail("usage: build_goreecloud_gallery_gc12.py <gallery-root> <commons-root>")
    gallery = Path(sys.argv[1]).resolve()
    commons = Path(sys.argv[2]).resolve()
    verify(gallery, GALLERY_COMMIT, "Gallery")
    verify(commons, COMMONS_COMMIT, "Commons")
    update_version(gallery)
    write_browse_resources(gallery)
    patch_layout(gallery / "app/src/main/res/layout/activity_main.xml", mode="directories")
    patch_layout(gallery / "app/src/main/res/layout/activity_media.xml", mode="media")
    update_notice(gallery)
    validate(gallery)
    print(f"Applied GoreeCloud Gallery gc.12 Glaze browsing integration: {VERSION_NAME} ({VERSION_CODE}).")


if __name__ == "__main__":
    main()
