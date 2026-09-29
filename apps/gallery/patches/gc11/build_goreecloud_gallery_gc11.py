#!/usr/bin/env python3
"""Apply GoreeCloud Gallery gc.11 native Glaze UI 1.0 integration."""
from __future__ import annotations

import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ANDROID_NS = "http://schemas.android.com/apk/res/android"
ET.register_namespace("android", ANDROID_NS)
ET.register_namespace("app", "http://schemas.android.com/apk/res-auto")
ET.register_namespace("tools", "http://schemas.android.com/tools")

GALLERY_COMMIT = "b28299dc33821eee8d108a9880ce87876cf31443"
COMMONS_COMMIT = "acfd352df1a1852d17a5f77def8b7ad6e522a5b6"
VERSION_NAME = "1.0.0"
VERSION_CODE = "10011"
GLAZE_UI_VERSION = "1.0.0"
GLAZE_UI_REFERENCE = "d6e446fd8ef251259d16368d50aad90d9287a774"


def fail(message: str) -> None:
    raise SystemExit(f"gc.11 patch failed: {message}")


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


def write_glaze_resources(gallery: Path) -> None:
    values = gallery / "app/src/main/res/values"
    values_night = gallery / "app/src/main/res/values-night"
    values_sw600 = gallery / "app/src/main/res/values-sw600dp"
    values_sw840 = gallery / "app/src/main/res/values-sw840dp"
    drawable = gallery / "app/src/main/res/drawable"

    write(values / "goreecloud_glaze.xml", """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Glaze UI 1.0 semantic native-Android mapping. -->
    <color name="goreecloud_glaze_canvas">#EEF3F9</color>
    <color name="goreecloud_glaze_canvas_accent">#F8FBFF</color>
    <color name="goreecloud_glaze_surface">#F2FFFFFF</color>
    <color name="goreecloud_glaze_surface_muted">#DCF4F7FB</color>
    <color name="goreecloud_glaze_text">#172033</color>
    <color name="goreecloud_glaze_muted">#67748A</color>
    <color name="goreecloud_glaze_line">#295F7492</color>
    <color name="goreecloud_glaze_accent">#366CF6</color>
    <color name="goreecloud_glaze_accent_secondary">#7C5CFF</color>
    <color name="goreecloud_glaze_success">#197A4B</color>
    <color name="goreecloud_glaze_warning">#A15C00</color>
    <color name="goreecloud_glaze_danger">#B42318</color>

    <dimen name="goreecloud_glaze_space_1">4dp</dimen>
    <dimen name="goreecloud_glaze_space_2">8dp</dimen>
    <dimen name="goreecloud_glaze_space_3">12dp</dimen>
    <dimen name="goreecloud_glaze_space_4">16dp</dimen>
    <dimen name="goreecloud_glaze_space_6">24dp</dimen>
    <dimen name="goreecloud_glaze_radius_sm">10dp</dimen>
    <dimen name="goreecloud_glaze_radius_md">14dp</dimen>
    <dimen name="goreecloud_glaze_radius_control">16dp</dimen>
    <dimen name="goreecloud_glaze_radius_lg">22dp</dimen>
    <dimen name="goreecloud_glaze_target_minimum">44dp</dimen>
    <dimen name="goreecloud_glaze_target_comfortable">48dp</dimen>
    <dimen name="goreecloud_glaze_focus_width">3dp</dimen>
    <dimen name="goreecloud_glaze_settings_horizontal_inset">16dp</dimen>

    <integer name="goreecloud_glaze_motion_instant">90</integer>
    <integer name="goreecloud_glaze_motion_fast">160</integer>
    <integer name="goreecloud_glaze_motion_standard">220</integer>
    <integer name="goreecloud_glaze_motion_emphasized">320</integer>
</resources>
""")

    write(values_night / "goreecloud_glaze.xml", """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="goreecloud_glaze_canvas">#0D1119</color>
    <color name="goreecloud_glaze_canvas_accent">#121824</color>
    <color name="goreecloud_glaze_surface">#F5181F2C</color>
    <color name="goreecloud_glaze_surface_muted">#DB1F2736</color>
    <color name="goreecloud_glaze_text">#F3F6FB</color>
    <color name="goreecloud_glaze_muted">#A1AEC0</color>
    <color name="goreecloud_glaze_line">#1FC1CFE5</color>
    <color name="goreecloud_glaze_accent">#7AA2FF</color>
    <color name="goreecloud_glaze_accent_secondary">#A594FF</color>
    <color name="goreecloud_glaze_success">#73D9A2</color>
    <color name="goreecloud_glaze_warning">#F7C36C</color>
    <color name="goreecloud_glaze_danger">#FF8A80</color>
</resources>
""")

    write(values_sw600 / "goreecloud_glaze.xml", """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <dimen name="goreecloud_glaze_settings_horizontal_inset">32dp</dimen>
</resources>
""")
    write(values_sw840 / "goreecloud_glaze.xml", """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <dimen name="goreecloud_glaze_settings_horizontal_inset">64dp</dimen>
</resources>
""")

    write(drawable / "goreecloud_glaze_canvas.xml", """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient
        android:angle="315"
        android:startColor="@color/goreecloud_glaze_canvas"
        android:centerColor="@color/goreecloud_glaze_canvas_accent"
        android:endColor="@color/goreecloud_glaze_canvas" />
</shape>
""")

    write(drawable / "goreecloud_glaze_toolbar.xml", """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient
        android:angle="0"
        android:startColor="@color/goreecloud_glaze_accent"
        android:endColor="@color/goreecloud_glaze_accent_secondary" />
</shape>
""")

    write(drawable / "goreecloud_glaze_settings_item.xml", """<?xml version="1.0" encoding="utf-8"?>
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


def patch_settings_layout(gallery: Path) -> None:
    path = gallery / "app/src/main/res/layout/activity_settings.xml"
    tree = ET.parse(path)
    root = tree.getroot()
    aid = f"{{{ANDROID_NS}}}id"
    abg = f"{{{ANDROID_NS}}}background"
    amin = f"{{{ANDROID_NS}}}minHeight"
    amargin_start = f"{{{ANDROID_NS}}}layout_marginStart"
    amargin_end = f"{{{ANDROID_NS}}}layout_marginEnd"
    amargin_top = f"{{{ANDROID_NS}}}layout_marginTop"
    apad_start = f"{{{ANDROID_NS}}}paddingStart"
    apad_end = f"{{{ANDROID_NS}}}paddingEnd"
    apad_bottom = f"{{{ANDROID_NS}}}paddingBottom"
    aclip = f"{{{ANDROID_NS}}}clipToPadding"

    root.set(abg, "@drawable/goreecloud_glaze_canvas")

    toolbar_found = False
    holder_found = False
    row_count = 0
    for element in root.iter():
        view_id = element.attrib.get(aid, "")
        if view_id == "@+id/settings_toolbar":
            element.set(abg, "@drawable/goreecloud_glaze_toolbar")
            element.set(amin, "@dimen/goreecloud_glaze_target_comfortable")
            toolbar_found = True
        elif view_id == "@+id/settings_nested_scrollview":
            element.set(aclip, "false")
        elif view_id == "@+id/settings_holder":
            element.set(apad_start, "@dimen/goreecloud_glaze_settings_horizontal_inset")
            element.set(apad_end, "@dimen/goreecloud_glaze_settings_horizontal_inset")
            element.set(apad_bottom, "@dimen/goreecloud_glaze_space_6")
            holder_found = True
        elif view_id.startswith("@+id/settings_") and view_id.endswith("_holder"):
            element.set(amin, "@dimen/goreecloud_glaze_target_comfortable")
            element.set(abg, "@drawable/goreecloud_glaze_settings_item")
            element.set(amargin_start, "@dimen/goreecloud_glaze_space_1")
            element.set(amargin_end, "@dimen/goreecloud_glaze_space_1")
            element.set(amargin_top, "@dimen/goreecloud_glaze_space_1")
            row_count += 1

    if not toolbar_found or not holder_found or row_count < 10:
        fail(f"settings Glaze mapping incomplete: toolbar={toolbar_found}, holder={holder_found}, rows={row_count}")

    ET.indent(tree, space="    ")
    tree.write(path, encoding="unicode", xml_declaration=True)


def update_notice(gallery: Path) -> None:
    notice_path = gallery / "GOREECLOUD-NOTICE.md"
    notice = read(notice_path).rstrip() + f"""

gc.11 deepens the native Android mapping to Glaze UI {GLAZE_UI_VERSION}, based on canonical GoreeCloud/glaze-ui reference revision {GLAZE_UI_REFERENCE}. Gallery now carries explicit semantic light/dark colors, spacing, radii, minimum and comfortable touch targets, motion timing resources, responsive settings insets, a layered canvas treatment, a GoreeCloud accent-gradient toolbar, and raised rounded settings rows with Android ripple feedback. The implementation preserves platform-native Android behavior and uses solid/native drawable fallbacks rather than depending on web-only blur effects or remote UI resources.

The Settings screen uses Compact-first 16dp horizontal composition, increases to 32dp at Android sw600dp and 64dp at sw840dp, and enforces a 48dp comfortable minimum height for interactive setting rows. These resources are a native semantic mapping rather than a copy of the canonical web CSS.
"""
    write(notice_path, notice)
    write(gallery / "app/src/main/assets/goreecloud_notice.txt", notice)


def validate(gallery: Path) -> None:
    layout = read(gallery / "app/src/main/res/layout/activity_settings.xml")
    values = read(gallery / "app/src/main/res/values/goreecloud_glaze.xml")
    night = read(gallery / "app/src/main/res/values-night/goreecloud_glaze.xml")
    sw600 = read(gallery / "app/src/main/res/values-sw600dp/goreecloud_glaze.xml")
    sw840 = read(gallery / "app/src/main/res/values-sw840dp/goreecloud_glaze.xml")
    properties = read(gallery / "gradle.properties")

    required_layout = (
        "@drawable/goreecloud_glaze_canvas",
        "@drawable/goreecloud_glaze_toolbar",
        "@drawable/goreecloud_glaze_settings_item",
        "@dimen/goreecloud_glaze_target_comfortable",
        "@dimen/goreecloud_glaze_settings_horizontal_inset",
    )
    for value in required_layout:
        if value not in layout:
            fail(f"missing Glaze layout invariant: {value}")

    for value in (
        "goreecloud_glaze_canvas",
        "goreecloud_glaze_surface",
        "goreecloud_glaze_accent",
        "goreecloud_glaze_target_minimum\">44dp",
        "goreecloud_glaze_target_comfortable\">48dp",
        "goreecloud_glaze_motion_instant\">90",
        "goreecloud_glaze_motion_fast\">160",
        "goreecloud_glaze_motion_standard\">220",
        "goreecloud_glaze_motion_emphasized\">320",
    ):
        if value not in values:
            fail(f"missing Glaze semantic invariant: {value}")

    if "#0D1119" not in night or "#7AA2FF" not in night:
        fail("missing Glaze dark-theme mapping")
    if ">32dp<" not in sw600 or ">64dp<" not in sw840:
        fail("missing Glaze adaptive settings insets")
    for required in (f"VERSION_NAME={VERSION_NAME}", f"VERSION_CODE={VERSION_CODE}"):
        if required not in properties:
            fail(f"missing version invariant: {required}")


def main() -> None:
    if len(sys.argv) != 3:
        fail("usage: build_goreecloud_gallery_gc11.py <gallery-root> <commons-root>")
    gallery = Path(sys.argv[1]).resolve()
    commons = Path(sys.argv[2]).resolve()
    verify(gallery, GALLERY_COMMIT, "Gallery")
    verify(commons, COMMONS_COMMIT, "Commons")
    update_version(gallery)
    write_glaze_resources(gallery)
    patch_settings_layout(gallery)
    update_notice(gallery)
    validate(gallery)
    print(f"Applied GoreeCloud Gallery gc.11 Glaze UI {GLAZE_UI_VERSION} integration: {VERSION_NAME} ({VERSION_CODE}).")


if __name__ == "__main__":
    main()
