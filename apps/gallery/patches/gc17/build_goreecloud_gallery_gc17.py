#!/usr/bin/env python3
"""Apply GoreeCloud Gallery gc.17 folder-delete and transient-surface fixes."""
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
VERSION_CODE = "10017"


def fail(message: str) -> None:
    raise SystemExit(f"gc.17 patch failed: {message}")


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
    content = read(path).replace("VERSION_CODE=10016", f"VERSION_CODE={VERSION_CODE}", 1)
    if f"VERSION_CODE={VERSION_CODE}" not in content:
        fail("could not update versionCode")
    write(path, content)


def patch_confirm_delete_layout(gallery: Path) -> None:
    path = gallery / "app/src/main/res/layout/dialog_confirm_delete_folder.xml"
    tree = ET.parse(path)
    root = tree.getroot()
    height = f"{{{ANDROID_NS}}}layout_height"
    background = f"{{{ANDROID_NS}}}background"

    root.set(height, "wrap_content")
    root.set(background, "@android:color/transparent")

    ET.indent(tree, space="    ")
    tree.write(path, encoding="unicode", xml_declaration=True)


def patch_confirm_delete_window(gallery: Path) -> None:
    path = gallery / "app/src/main/kotlin/org/fossify/gallery/dialogs/ConfirmDeleteFolderDialog.kt"
    content = read(path)

    if "import org.fossify.gallery.R" not in content:
        content = content.replace(
            "import org.fossify.gallery.databinding.DialogConfirmDeleteFolderBinding\n",
            "import org.fossify.gallery.R\nimport org.fossify.gallery.databinding.DialogConfirmDeleteFolderBinding\n",
            1,
        )

    old = "alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)"
    new = """alertDialog.window?.setBackgroundDrawableResource(R.drawable.goreecloud_glaze_dialog_surface)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(activity.getColor(R.color.goreecloud_glaze_danger))
                    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(activity.getColor(R.color.goreecloud_glaze_accent))"""
    if old not in content:
        fail("gc.15 confirm-delete window customization not found")
    content = content.replace(old, new, 1)
    write(path, content)


def patch_explicit_folder_delete(gallery: Path) -> None:
    path = gallery / "app/src/main/kotlin/org/fossify/gallery/activities/MainActivity.kt"
    content = read(path)
    old = """                if (config.deleteEmptyFolders) {
                    folders.filter {
                        !it.absolutePath.isDownloadsFolder()
                                && it.isDirectory
                                && it.toFileDirItem(this).getProperFileCount(this, true) == 0
                    }
                        .forEach {
                            tryDeleteFileDirItem(it.toFileDirItem(this), true, true)
                        }
                }
"""
    new = """                // An explicit folder-delete action must remove the selected folder once its
                // deletable media has been removed or moved to the recycle bin. The global
                // deleteEmptyFolders preference controls incidental cleanup, not whether a folder
                // the user explicitly chose to delete is left behind as an empty shell.
                folders.filter {
                    !it.absolutePath.isDownloadsFolder()
                            && it.isDirectory
                            && it.toFileDirItem(this).getProperFileCount(this, true) == 0
                }
                    .forEach {
                        tryDeleteFileDirItem(it.toFileDirItem(this), true, true)
                    }
"""
    if content.count(old) != 1:
        fail("explicit folder-delete cleanup block not found")
    content = content.replace(old, new, 1)
    write(path, content)


def inject_style_items(content: str, style_name: str, items: tuple[str, ...]) -> tuple[str, bool]:
    pattern = re.compile(rf'(<style\s+name="{re.escape(style_name)}"[^>]*>)(.*?)(</style>)', re.S)
    match = pattern.search(content)
    if not match:
        return content, False

    body = match.group(2)
    for item in items:
        attr_name = item.split('name="', 1)[1].split('"', 1)[0]
        item_pattern = re.compile(
            rf'<item\s+name="{re.escape(attr_name)}"\s*>.*?</item>',
            re.S,
        )
        if item_pattern.search(body):
            body = item_pattern.sub(item, body, count=1)
        else:
            body = body.rstrip() + f"\n        {item}\n"

    replacement = match.group(1) + body + match.group(3)
    return content[: match.start()] + replacement + content[match.end() :], True


def patch_popup_contrast(commons: Path) -> None:
    values_dir = commons / "commons/src/main/res/values"
    style_files = sorted(values_dir.glob("*.xml"))
    dark_found = False
    light_found = False

    dark_items = (
        '<item name="android:textColor">#F3F6FB</item>',
        '<item name="android:textColorPrimary">#F3F6FB</item>',
        '<item name="android:textColorSecondary">#A1AEC0</item>',
    )
    light_items = (
        '<item name="android:textColor">#172033</item>',
        '<item name="android:textColorPrimary">#172033</item>',
        '<item name="android:textColorSecondary">#67748A</item>',
    )

    for path in style_files:
        content = read(path)
        changed = False
        for style_name in ("GoreeCloudGalleryPopupThemeDark", "TopPopupMenu.Overflow.Dark", "AppTheme.PopupMenuDarkStyle"):
            content, found = inject_style_items(content, style_name, dark_items)
            dark_found = dark_found or found
            changed = changed or found
        for style_name in ("GoreeCloudGalleryPopupThemeLight", "TopPopupMenu.Overflow.Light", "AppTheme.PopupMenuLightStyle"):
            content, found = inject_style_items(content, style_name, light_items)
            light_found = light_found or found
            changed = changed or found
        if changed:
            write(path, content)

    if not dark_found or not light_found:
        fail(f"popup theme styles not found: dark={dark_found}, light={light_found}")

    drawable = commons / "commons/src/main/res/drawable"
    dark_surface = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#202333" />
    <stroke android:width="1dp" android:color="#557AA2FF" />
    <corners android:radius="20dp" />
    <padding android:left="8dp" android:top="8dp" android:right="8dp" android:bottom="8dp" />
</shape>
"""
    light_surface = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#F4F3FF" />
    <stroke android:width="1dp" android:color="#337C5CFF" />
    <corners android:radius="20dp" />
    <padding android:left="8dp" android:top="8dp" android:right="8dp" android:bottom="8dp" />
</shape>
"""
    for name in ("goreecloud_gallery_popup_bg_dark.xml", "top_popup_menu_bg_dark.xml"):
        write(drawable / name, dark_surface)
    for name in ("goreecloud_gallery_popup_bg_light.xml", "top_popup_menu_bg_light.xml"):
        write(drawable / name, light_surface)


def update_notice(gallery: Path) -> None:
    path = gallery / "GOREECLOUD-NOTICE.md"
    notice = read(path).rstrip() + """

gc.17 responds to representative-device acceptance findings in gc.16. Contextual and toolbar overflow menus now enforce readable foreground colors against their light and dark Glaze popup surfaces. The folder-delete confirmation now uses content-driven geometry and one coherent rounded Glaze window surface, including the action-button area. Explicit folder deletion also removes a selected folder once its deletable media is gone, independent of the incidental empty-folder cleanup preference, while preserving the Downloads-folder safety boundary and refusing to remove folders that still contain data.
"""
    write(path, notice)
    write(gallery / "app/src/main/assets/goreecloud_notice.txt", notice)


def validate(gallery: Path, commons: Path) -> None:
    props = read(gallery / "gradle.properties")
    confirm_layout = read(gallery / "app/src/main/res/layout/dialog_confirm_delete_folder.xml")
    confirm_class = read(gallery / "app/src/main/kotlin/org/fossify/gallery/dialogs/ConfirmDeleteFolderDialog.kt")
    main_activity = read(gallery / "app/src/main/kotlin/org/fossify/gallery/activities/MainActivity.kt")
    notice = read(gallery / "GOREECLOUD-NOTICE.md")
    styles = "\n".join(read(path) for path in sorted((commons / "commons/src/main/res/values").glob("*.xml")))
    popup_dark = read(commons / "commons/src/main/res/drawable/goreecloud_gallery_popup_bg_dark.xml")
    popup_light = read(commons / "commons/src/main/res/drawable/goreecloud_gallery_popup_bg_light.xml")

    if f"VERSION_NAME={VERSION_NAME}" not in props or f"VERSION_CODE={VERSION_CODE}" not in props:
        fail("version identity mismatch")
    if 'android:layout_height="wrap_content"' not in confirm_layout:
        fail("confirm-delete dialog is not content-driven")
    if 'android:background="@android:color/transparent"' not in confirm_layout:
        fail("confirm-delete content surface is not delegated to the dialog window")
    if "R.drawable.goreecloud_glaze_dialog_surface" not in confirm_class:
        fail("confirm-delete Glaze window surface is missing")
    if "AlertDialog.BUTTON_POSITIVE" not in confirm_class or "goreecloud_glaze_danger" not in confirm_class:
        fail("destructive confirm button semantics are missing")
    if "if (config.deleteEmptyFolders)" in main_activity[main_activity.find("private fun deleteFilteredFileDirItems"):main_activity.find("private fun setupLayoutManager")]:
        fail("explicit folder deletion still depends on deleteEmptyFolders preference")
    if "the user explicitly chose to delete is left behind as an empty shell" not in main_activity:
        fail("explicit folder-delete contract is missing")
    for required in ("GoreeCloudGalleryPopupThemeDark", "#F3F6FB", "#172033"):
        if required not in styles:
            fail(f"popup contrast invariant missing: {required}")
    if "#202333" not in popup_dark or "20dp" not in popup_dark:
        fail("dark Glaze popup surface missing")
    if "#F4F3FF" not in popup_light or "20dp" not in popup_light:
        fail("light Glaze popup surface missing")
    if "gc.17 responds to representative-device acceptance findings" not in notice:
        fail("gc.17 notice missing")


def main() -> None:
    if len(sys.argv) != 3:
        fail("usage: build_goreecloud_gallery_gc17.py <gallery-root> <commons-root>")
    gallery = Path(sys.argv[1]).resolve()
    commons = Path(sys.argv[2]).resolve()
    verify(gallery, GALLERY_COMMIT, "Gallery")
    verify(commons, COMMONS_COMMIT, "Commons")
    update_version(gallery)
    patch_confirm_delete_layout(gallery)
    patch_confirm_delete_window(gallery)
    patch_explicit_folder_delete(gallery)
    patch_popup_contrast(commons)
    update_notice(gallery)
    validate(gallery, commons)
    print(f"Applied GoreeCloud Gallery gc.17 delete/transient-surface fixes: {VERSION_NAME} ({VERSION_CODE}).")


if __name__ == "__main__":
    main()
