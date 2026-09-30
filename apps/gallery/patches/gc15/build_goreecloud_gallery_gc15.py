#!/usr/bin/env python3
"""Apply GoreeCloud Gallery gc.15 transient-surface and Settings refinements."""
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
VERSION_CODE = "10015"
GLAZE_UI_VERSION = "1.0.0"
GLAZE_UI_REFERENCE = "d6e446fd8ef251259d16368d50aad90d9287a774"


def fail(message: str) -> None:
    raise SystemExit(f"gc.15 patch failed: {message}")


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


def write_surfaces(gallery: Path, commons: Path) -> None:
    drawable = gallery / "app/src/main/res/drawable"
    write(drawable / "goreecloud_glaze_dialog_surface.xml", """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/goreecloud_glaze_surface" />
    <stroke android:width="1dp" android:color="@color/goreecloud_glaze_line" />
    <corners android:radius="@dimen/goreecloud_glaze_radius_lg" />
    <padding android:left="8dp" android:top="6dp" android:right="8dp" android:bottom="6dp" />
</shape>
""")

    commons_drawable = commons / "commons/src/main/res/drawable"
    write(commons_drawable / "goreecloud_gallery_popup_bg_light.xml", """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#F4F3FF" />
    <stroke android:width="1dp" android:color="#337C5CFF" />
    <corners android:radius="20dp" />
    <padding android:left="6dp" android:top="6dp" android:right="6dp" android:bottom="6dp" />
</shape>
""")
    write(commons_drawable / "goreecloud_gallery_popup_bg_dark.xml", """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#202333" />
    <stroke android:width="1dp" android:color="#557AA2FF" />
    <corners android:radius="20dp" />
    <padding android:left="6dp" android:top="6dp" android:right="6dp" android:bottom="6dp" />
</shape>
""")


def patch_dialog_layout(path: Path) -> None:
    tree = ET.parse(path)
    root = tree.getroot()
    aid = f"{{{ANDROID_NS}}}id"
    abg = f"{{{ANDROID_NS}}}background"
    amin = f"{{{ANDROID_NS}}}minHeight"
    abutton_tint = f"{{{ANDROID_NS}}}buttonTint"
    avis = f"{{{ANDROID_NS}}}visibility"
    atext_color = f"{{{ANDROID_NS}}}textColor"
    apad_top = f"{{{ANDROID_NS}}}paddingTop"
    apad_bottom = f"{{{ANDROID_NS}}}paddingBottom"

    root.set(abg, "@drawable/goreecloud_glaze_dialog_surface")
    interactive = 0
    for element in root.iter():
        view_id = element.attrib.get(aid, "")
        tag = element.tag
        if tag.endswith("RadioButton") or tag.endswith("Checkbox"):
            element.set(amin, "@dimen/goreecloud_glaze_target_comfortable")
            element.set(abutton_tint, "@color/goreecloud_glaze_accent")
            element.set(apad_top, "4dp")
            element.set(apad_bottom, "4dp")
            interactive += 1
        if "divider" in view_id:
            element.set(avis, "gone")
        if view_id.endswith("bottom_note"):
            element.set(atext_color, "@color/goreecloud_glaze_muted")
    if interactive == 0:
        fail(f"no interactive controls found in {path.name}")
    ET.indent(tree, space="    ")
    tree.write(path, encoding="unicode", xml_declaration=True)


def patch_confirm_delete(gallery: Path) -> None:
    layout = gallery / "app/src/main/res/layout/dialog_confirm_delete_folder.xml"
    tree = ET.parse(layout)
    root = tree.getroot()
    aid = f"{{{ANDROID_NS}}}id"
    abg = f"{{{ANDROID_NS}}}background"
    atc = f"{{{ANDROID_NS}}}textColor"
    root.set(abg, "@drawable/goreecloud_glaze_dialog_surface")
    found = False
    for element in root.iter():
        if element.attrib.get(aid, "") == "@+id/message_warning":
            element.set(atc, "@color/goreecloud_glaze_danger")
            found = True
    if not found:
        fail("confirm-delete warning view not found")
    ET.indent(tree, space="    ")
    tree.write(layout, encoding="unicode", xml_declaration=True)

    cls = gallery / "app/src/main/kotlin/org/fossify/gallery/dialogs/ConfirmDeleteFolderDialog.kt"
    content = read(cls)
    needle = "dialog = alertDialog"
    if needle not in content:
        fail("confirm-delete dialog callback not found")
    content = content.replace(needle, needle + "\n                    alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)", 1)
    write(cls, content)


def patch_dialog_windows(gallery: Path) -> None:
    for name in ("ChangeSortingDialog.kt", "ChangeGroupingDialog.kt", "FilterMediaDialog.kt"):
        path = gallery / "app/src/main/kotlin/org/fossify/gallery/dialogs" / name
        content = read(path)
        pattern = re.compile(r"activity\.setupDialogStuff\(binding\.root, this, ([^)]+)\)")
        replacement = r"activity.setupDialogStuff(binding.root, this, \1) { alertDialog ->\n                    alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)\n                }"
        content, count = pattern.subn(replacement, content, count=1)
        if count != 1:
            fail(f"could not patch dialog window in {name}")
        write(path, content)


def patch_settings(gallery: Path) -> None:
    path = gallery / "app/src/main/res/layout/activity_settings.xml"
    tree = ET.parse(path)
    root = tree.getroot()
    aid = f"{{{ANDROID_NS}}}id"
    abg = f"{{{ANDROID_NS}}}background"
    amin = f"{{{ANDROID_NS}}}minHeight"
    avis = f"{{{ANDROID_NS}}}visibility"
    amt = f"{{{ANDROID_NS}}}layout_marginTop"
    apad_top = f"{{{ANDROID_NS}}}paddingTop"
    apad_bottom = f"{{{ANDROID_NS}}}paddingBottom"

    row_count = 0
    toolbar_count = 0
    for element in root.iter():
        view_id = element.attrib.get(aid, "")
        tag = element.tag
        if tag.endswith("AppBarLayout") or tag.endswith("MaterialToolbar") or view_id == "@+id/settings_toolbar":
            element.set(abg, "@drawable/goreecloud_glaze_toolbar")
            element.set(amin, "@dimen/goreecloud_glaze_target_comfortable")
            toolbar_count += 1
        if view_id.startswith("@+id/settings_") and view_id.endswith("_holder"):
            element.set(amin, "@dimen/goreecloud_glaze_target_comfortable")
            element.set(amt, "2dp")
            element.set(apad_top, "2dp")
            element.set(apad_bottom, "2dp")
            row_count += 1
        if "divider" in view_id and view_id.startswith("@+id/settings_"):
            element.set(avis, "gone")
    if row_count < 10 or toolbar_count == 0:
        fail(f"settings refinement incomplete: rows={row_count}, toolbars={toolbar_count}")
    ET.indent(tree, space="    ")
    tree.write(path, encoding="unicode", xml_declaration=True)


def patch_folder_hierarchy(gallery: Path) -> None:
    for name in ("directory_item_grid_rounded_corners.xml", "directory_item_list.xml"):
        path = gallery / "app/src/main/res/layout" / name
        tree = ET.parse(path)
        root = tree.getroot()
        aid = f"{{{ANDROID_NS}}}id"
        atc = f"{{{ANDROID_NS}}}textColor"
        ats = f"{{{ANDROID_NS}}}textSize"
        ast = f"{{{ANDROID_NS}}}textStyle"
        apt = f"{{{ANDROID_NS}}}paddingTop"
        found_name = False
        found_count = False
        for element in root.iter():
            view_id = element.attrib.get(aid, "")
            if view_id == "@+id/dir_name":
                element.set(atc, "@color/goreecloud_glaze_text")
                element.set(ast, "bold")
                element.set(apt, "6dp")
                found_name = True
            elif view_id == "@+id/photo_cnt":
                element.set(atc, "@color/goreecloud_glaze_muted")
                element.set(ats, "@dimen/smaller_text_size")
                element.set(apt, "2dp")
                found_count = True
        if not found_name or not found_count:
            fail(f"folder hierarchy views missing in {name}")
        ET.indent(tree, space="    ")
        tree.write(path, encoding="unicode", xml_declaration=True)


def update_notice(gallery: Path) -> None:
    path = gallery / "GOREECLOUD-NOTICE.md"
    notice = read(path).rstrip() + f"""

gc.15 refines the native Glaze UI {GLAZE_UI_VERSION} mapping using canonical reference revision {GLAZE_UI_REFERENCE} in response to representative-device visual review. Sorting, grouping, filter, and destructive confirmation surfaces now use local rounded Glaze presentation with semantic accents and comfortable controls; overflow menus use coordinated light/dark Glaze popup surfaces; Settings reduces redundant dividers and excessive card spacing while strengthening its Glaze app-bar treatment; folder names and item counts use stronger primary/secondary semantic hierarchy. Existing media density, permission behavior, sorting/grouping semantics, destructive confirmations, privacy boundaries, and offline operation remain unchanged.
"""
    write(path, notice)
    write(gallery / "app/src/main/assets/goreecloud_notice.txt", notice)


def validate(gallery: Path, commons: Path) -> None:
    props = read(gallery / "gradle.properties")
    settings = read(gallery / "app/src/main/res/layout/activity_settings.xml")
    sorting = read(gallery / "app/src/main/res/layout/dialog_change_sorting.xml")
    grouping = read(gallery / "app/src/main/res/layout/dialog_change_grouping.xml")
    filtering = read(gallery / "app/src/main/res/layout/dialog_filter_media.xml")
    confirm = read(gallery / "app/src/main/res/layout/dialog_confirm_delete_folder.xml")
    folder_grid = read(gallery / "app/src/main/res/layout/directory_item_grid_rounded_corners.xml")
    popup_light = read(commons / "commons/src/main/res/drawable/goreecloud_gallery_popup_bg_light.xml")
    popup_dark = read(commons / "commons/src/main/res/drawable/goreecloud_gallery_popup_bg_dark.xml")
    notice = read(gallery / "GOREECLOUD-NOTICE.md")

    for required in (f"VERSION_NAME={VERSION_NAME}", f"VERSION_CODE={VERSION_CODE}"):
        if required not in props:
            fail(f"missing version invariant: {required}")
    for content in (sorting, grouping, filtering):
        if "@drawable/goreecloud_glaze_dialog_surface" not in content:
            fail("Glaze dialog surface missing")
        if "@color/goreecloud_glaze_accent" not in content:
            fail("semantic dialog accent missing")
        if "@dimen/goreecloud_glaze_target_comfortable" not in content:
            fail("comfortable dialog targets missing")
    if "@color/goreecloud_glaze_danger" not in confirm:
        fail("semantic destructive warning color missing")
    if settings.count('android:visibility="gone"') < 2:
        fail("redundant Settings dividers were not reduced")
    if "@drawable/goreecloud_glaze_toolbar" not in settings:
        fail("Settings Glaze header treatment missing")
    if "@color/goreecloud_glaze_text" not in folder_grid or "@color/goreecloud_glaze_muted" not in folder_grid:
        fail("folder primary/secondary semantic hierarchy missing")
    if "@dimen/smaller_text_size" not in folder_grid or 'android:textStyle="bold"' not in folder_grid:
        fail("folder typography hierarchy missing")
    if "#F4F3FF" not in popup_light or "20dp" not in popup_light:
        fail("light Glaze popup surface missing")
    if "#202333" not in popup_dark or "20dp" not in popup_dark:
        fail("dark Glaze popup surface missing")
    if "gc.15 refines the native Glaze UI" not in notice:
        fail("gc.15 notice missing")


def main() -> None:
    if len(sys.argv) != 3:
        fail("usage: build_goreecloud_gallery_gc15.py <gallery-root> <commons-root>")
    gallery = Path(sys.argv[1]).resolve()
    commons = Path(sys.argv[2]).resolve()
    verify(gallery, GALLERY_COMMIT, "Gallery")
    verify(commons, COMMONS_COMMIT, "Commons")
    update_version(gallery)
    write_surfaces(gallery, commons)
    for name in ("dialog_change_sorting.xml", "dialog_change_grouping.xml", "dialog_filter_media.xml"):
        patch_dialog_layout(gallery / "app/src/main/res/layout" / name)
    patch_confirm_delete(gallery)
    patch_dialog_windows(gallery)
    patch_settings(gallery)
    patch_folder_hierarchy(gallery)
    update_notice(gallery)
    validate(gallery, commons)
    print(f"Applied GoreeCloud Gallery gc.15 Glaze transient/settings refinement: {VERSION_NAME} ({VERSION_CODE}).")


if __name__ == "__main__":
    main()
