#!/usr/bin/env python3
"""Apply GoreeCloud Gallery gc.10 settings simplification and privacy-access cleanup."""
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
VERSION_CODE = "10010"


def fail(message: str) -> None:
    raise SystemExit(f"gc.10 patch failed: {message}")


def read(path: Path) -> str:
    if not path.is_file():
        fail(f"missing {path}")
    return path.read_text(encoding="utf-8")


def write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    content = read(path)
    if content.count(old) != 1:
        fail(f"expected one {label} match in {path}")
    write(path, content.replace(old, new, 1))


def regex_once(path: Path, pattern: str, replacement: str, label: str) -> None:
    content, count = re.subn(pattern, replacement, read(path), count=1, flags=re.S)
    if count != 1:
        fail(f"could not patch {label} in {path}")
    write(path, content)


def verify(root: Path, expected: str, label: str) -> None:
    actual = subprocess.check_output(
        ["git", "-C", str(root), "rev-parse", "HEAD"], text=True
    ).strip()
    if actual != expected:
        fail(f"{label} checkout is {actual}, expected {expected}")


def update_version(gallery: Path) -> None:
    path = gallery / "gradle.properties"
    content = read(path)
    content, name_count = re.subn(
        r"^VERSION_NAME=.*$", f"VERSION_NAME={VERSION_NAME}", content, count=1, flags=re.M
    )
    content, code_count = re.subn(
        r"^VERSION_CODE=.*$", f"VERSION_CODE={VERSION_CODE}", content, count=1, flags=re.M
    )
    if name_count != 1 or code_count != 1:
        fail("could not update version metadata")
    write(path, content)


def remove_view(path: Path, view_id: str) -> None:
    aid = f"{{{ANDROID_NS}}}id"
    tree = ET.parse(path)
    root = tree.getroot()

    def walk(parent: ET.Element) -> bool:
        for child in list(parent):
            if child.attrib.get(aid) == f"@+id/{view_id}":
                parent.remove(child)
                return True
            if walk(child):
                return True
        return False

    if not walk(root):
        fail(f"missing view {view_id} in {path}")
    ET.indent(tree, space="    ")
    tree.write(path, encoding="unicode", xml_declaration=True)


def patch_settings_activity(gallery: Path) -> None:
    path = gallery / "app/src/main/kotlin/org/fossify/gallery/activities/SettingsActivity.kt"

    replace_once(
        path,
        "import android.content.Intent\nimport android.os.Bundle\n",
        "import android.content.Intent\nimport android.net.Uri\nimport android.os.Bundle\nimport android.provider.Settings\n",
        "Android settings imports",
    )

    replace_once(path, "        setupFileThumbnailStyle()\n", "", "file thumbnail setup call")
    replace_once(path, "        setupFolderThumbnailStyle()\n", "", "folder thumbnail setup call")

    regex_once(
        path,
        r"\n    private fun setupFileThumbnailStyle\(\) \{.*?\n    \}\n",
        "\n",
        "file thumbnail settings handler",
    )
    regex_once(
        path,
        r"\n    private fun setupFolderThumbnailStyle\(\) \{.*?\n    \}\n",
        "\n",
        "folder thumbnail settings handler",
    )

    regex_once(
        path,
        r"    private fun setupCustomizeColors\(\) \{\n.*?\n    \}\n",
        """    private fun setupCustomizeColors() {
        binding.settingsColorCustomizationHolder.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
""",
        "privacy and permissions action",
    )


def patch_settings_layout(gallery: Path) -> None:
    path = gallery / "app/src/main/res/layout/activity_settings.xml"
    tree = ET.parse(path)
    root = tree.getroot()
    aid = f"{{{ANDROID_NS}}}id"
    atext = f"{{{ANDROID_NS}}}text"

    replacements = {
        "@+id/settings_color_customization_section_label": "@string/goreecloud_privacy_permissions",
        "@+id/settings_color_customization_label": "@string/goreecloud_android_app_permissions",
    }

    found = set()
    for element in root.iter():
        view_id = element.attrib.get(aid)
        if view_id in replacements:
            element.set(atext, replacements[view_id])
            found.add(view_id)

    if found != set(replacements):
        fail("could not relabel privacy and permissions controls")

    ET.indent(tree, space="    ")
    tree.write(path, encoding="unicode", xml_declaration=True)

    # Upstream monetization acknowledgement is not part of the GoreeCloud-maintained fork,
    # and thumbnail shape controls are intentionally fixed by GoreeCloud presentation policy.
    remove_view(path, "settings_purchase_thank_you_holder")
    remove_view(path, "settings_file_thumbnail_style_holder")
    remove_view(path, "settings_folder_thumbnail_style_holder")


def patch_strings(gallery: Path) -> None:
    path = gallery / "app/src/main/res/values/strings.xml"
    tree = ET.parse(path)
    root = tree.getroot()

    wanted = {
        "goreecloud_privacy_permissions": "Privacy & permissions",
        "goreecloud_android_app_permissions": "Android app permissions",
    }
    existing = {node.attrib.get("name") for node in root.findall("string")}
    for name, value in wanted.items():
        if name in existing:
            fail(f"string already exists unexpectedly: {name}")
        node = ET.SubElement(root, "string", {"name": name})
        node.text = value

    ET.indent(tree, space="    ")
    tree.write(path, encoding="unicode", xml_declaration=True)


def update_notice(gallery: Path) -> None:
    notice_path = gallery / "GOREECLOUD-NOTICE.md"
    notice = read(notice_path).rstrip() + """

gc.10 simplifies Settings for the GoreeCloud-maintained build. The upstream color-customization entry now serves as a direct Privacy & permissions entry that opens Android's application settings, where media access and other OS-controlled permissions can be reviewed. Upstream donation acknowledgement is removed from the Settings surface. File and folder thumbnail-style rows are also removed because GoreeCloud already enforces rounded thumbnails and intentionally does not expose square-thumbnail presentation.

The remaining settings are retained only where they continue to change meaningful application behavior, including language and date/time presentation, folder inclusion and exclusion, hidden-media handling, search scope, video behavior, accessibility and gesture behavior, password protection, file operations, recycle-bin behavior, cache clearing, and portable import/export controls.
"""
    write(notice_path, notice)
    write(gallery / "app/src/main/assets/goreecloud_notice.txt", notice)


def validate(gallery: Path) -> None:
    settings = read(gallery / "app/src/main/kotlin/org/fossify/gallery/activities/SettingsActivity.kt")
    layout = read(gallery / "app/src/main/res/layout/activity_settings.xml")
    strings = read(gallery / "app/src/main/res/values/strings.xml")
    properties = read(gallery / "gradle.properties")

    required_settings = (
        "Settings.ACTION_APPLICATION_DETAILS_SETTINGS",
        'Uri.parse("package:$packageName")',
    )
    for value in required_settings:
        if value not in settings:
            fail(f"missing settings invariant: {value}")

    for forbidden in (
        "setupFileThumbnailStyle()",
        "setupFolderThumbnailStyle()",
        "settings_purchase_thank_you_holder",
        "settings_file_thumbnail_style_holder",
        "settings_folder_thumbnail_style_holder",
    ):
        if forbidden in settings or forbidden in layout:
            fail(f"removed setting returned: {forbidden}")

    for required in (
        'name="goreecloud_privacy_permissions"',
        'name="goreecloud_android_app_permissions"',
        "@string/goreecloud_privacy_permissions",
        "@string/goreecloud_android_app_permissions",
    ):
        if required not in strings and required not in layout:
            fail(f"missing privacy settings resource: {required}")

    for required in (f"VERSION_NAME={VERSION_NAME}", f"VERSION_CODE={VERSION_CODE}"):
        if required not in properties:
            fail(f"missing version invariant: {required}")


def main() -> None:
    if len(sys.argv) != 3:
        fail("usage: build_goreecloud_gallery_gc10.py <gallery-root> <commons-root>")

    gallery = Path(sys.argv[1]).resolve()
    commons = Path(sys.argv[2]).resolve()
    verify(gallery, GALLERY_COMMIT, "Gallery")
    verify(commons, COMMONS_COMMIT, "Commons")

    update_version(gallery)
    patch_settings_activity(gallery)
    patch_settings_layout(gallery)
    patch_strings(gallery)
    update_notice(gallery)
    validate(gallery)

    print(
        f"Applied GoreeCloud Gallery gc.10 settings cleanup and privacy access: "
        f"{VERSION_NAME} ({VERSION_CODE})."
    )


if __name__ == "__main__":
    main()
