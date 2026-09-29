#!/usr/bin/env python3
"""Apply GoreeCloud Gallery gc.7 direct MySearchMenu overflow-menu correction."""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

GALLERY_COMMIT = "b28299dc33821eee8d108a9880ce87876cf31443"
COMMONS_COMMIT = "acfd352df1a1852d17a5f77def8b7ad6e522a5b6"
VERSION_NAME = "1.0.0-gc.7"
VERSION_CODE = "10007"


def fail(message: str) -> None:
    raise SystemExit(f"gc.7 patch failed: {message}")


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


def verify(root: Path, expected: str, label: str) -> None:
    actual = subprocess.check_output(
        ["git", "-C", str(root), "rev-parse", "HEAD"], text=True
    ).strip()
    if actual != expected:
        fail(f"{label} checkout is {actual}, expected {expected}")


def update_version(gallery: Path) -> None:
    path = gallery / "gradle.properties"
    content = read(path)
    content, n1 = re.subn(
        r"^VERSION_NAME=.*$", f"VERSION_NAME={VERSION_NAME}", content, count=1, flags=re.M
    )
    content, n2 = re.subn(
        r"^VERSION_CODE=.*$", f"VERSION_CODE={VERSION_CODE}", content, count=1, flags=re.M
    )
    if n1 != 1 or n2 != 1:
        fail("could not update version metadata")
    write(path, content)


def add_popup_resources(commons: Path) -> None:
    values = commons / "commons/src/main/res/values/goreecloud_gallery_popup.xml"
    write(
        values,
        '''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!--
        GoreeCloud Gallery owns this toolbar-overflow theme instead of inheriting the
        Fossify Commons dark overflow style. MySearchMenu selects the correct theme
        directly from the effective application background before its menu is shown.
    -->
    <style name="GoreeCloudGalleryPopupTextLight" parent="TextAppearance.AppCompat.Body1">
        <item name="android:textColor">#14213D</item>
    </style>

    <style name="GoreeCloudGalleryPopupTextDark" parent="TextAppearance.AppCompat.Body1">
        <item name="android:textColor">#F5F7FC</item>
    </style>

    <style name="GoreeCloudGalleryPopupListLight" parent="@android:style/Widget.ListView.DropDown">
        <item name="android:background">@drawable/goreecloud_gallery_popup_bg_light</item>
        <item name="android:textColor">#14213D</item>
    </style>

    <style name="GoreeCloudGalleryPopupListDark" parent="@android:style/Widget.ListView.DropDown">
        <item name="android:background">@drawable/goreecloud_gallery_popup_bg_dark</item>
        <item name="android:textColor">#F5F7FC</item>
    </style>

    <style name="GoreeCloudGalleryPopupMenuLight" parent="@style/Widget.MaterialComponents.PopupMenu.Overflow">
        <item name="android:background">@drawable/goreecloud_gallery_popup_bg_light</item>
        <item name="android:popupBackground">@drawable/goreecloud_gallery_popup_bg_light</item>
        <item name="android:textColor">#14213D</item>
        <item name="android:textColorPrimary">#14213D</item>
        <item name="android:textColorSecondary">#475569</item>
        <item name="dropDownListViewStyle">@style/GoreeCloudGalleryPopupListLight</item>
    </style>

    <style name="GoreeCloudGalleryPopupMenuDark" parent="@style/Widget.MaterialComponents.PopupMenu.Overflow">
        <item name="android:background">@drawable/goreecloud_gallery_popup_bg_dark</item>
        <item name="android:popupBackground">@drawable/goreecloud_gallery_popup_bg_dark</item>
        <item name="android:textColor">#F5F7FC</item>
        <item name="android:textColorPrimary">#F5F7FC</item>
        <item name="android:textColorSecondary">#CBD5E1</item>
        <item name="dropDownListViewStyle">@style/GoreeCloudGalleryPopupListDark</item>
    </style>

    <style name="GoreeCloudGalleryPopupThemeLight" parent="ThemeOverlay.AppCompat.Light">
        <item name="android:colorBackground">#F7F8FC</item>
        <item name="android:textColor">#14213D</item>
        <item name="android:textColorPrimary">#14213D</item>
        <item name="android:textColorSecondary">#475569</item>
        <item name="colorControlNormal">#14213D</item>
        <item name="colorSurface">#F7F8FC</item>
        <item name="colorOnSurface">#14213D</item>
        <item name="actionOverflowMenuStyle">@style/GoreeCloudGalleryPopupMenuLight</item>
        <item name="android:actionOverflowMenuStyle">@style/GoreeCloudGalleryPopupMenuLight</item>
        <item name="popupMenuStyle">@style/GoreeCloudGalleryPopupMenuLight</item>
        <item name="android:popupMenuStyle">@style/GoreeCloudGalleryPopupMenuLight</item>
        <item name="textAppearanceLargePopupMenu">@style/GoreeCloudGalleryPopupTextLight</item>
        <item name="textAppearanceSmallPopupMenu">@style/GoreeCloudGalleryPopupTextLight</item>
        <item name="android:itemTextAppearance">@style/GoreeCloudGalleryPopupTextLight</item>
    </style>

    <style name="GoreeCloudGalleryPopupThemeDark" parent="ThemeOverlay.AppCompat.Dark">
        <item name="android:colorBackground">#171C29</item>
        <item name="android:textColor">#F5F7FC</item>
        <item name="android:textColorPrimary">#F5F7FC</item>
        <item name="android:textColorSecondary">#CBD5E1</item>
        <item name="colorControlNormal">#F5F7FC</item>
        <item name="colorSurface">#171C29</item>
        <item name="colorOnSurface">#F5F7FC</item>
        <item name="actionOverflowMenuStyle">@style/GoreeCloudGalleryPopupMenuDark</item>
        <item name="android:actionOverflowMenuStyle">@style/GoreeCloudGalleryPopupMenuDark</item>
        <item name="popupMenuStyle">@style/GoreeCloudGalleryPopupMenuDark</item>
        <item name="android:popupMenuStyle">@style/GoreeCloudGalleryPopupMenuDark</item>
        <item name="textAppearanceLargePopupMenu">@style/GoreeCloudGalleryPopupTextDark</item>
        <item name="textAppearanceSmallPopupMenu">@style/GoreeCloudGalleryPopupTextDark</item>
        <item name="android:itemTextAppearance">@style/GoreeCloudGalleryPopupTextDark</item>
    </style>
</resources>
''',
    )

    write(
        commons / "commons/src/main/res/drawable/goreecloud_gallery_popup_bg_light.xml",
        '''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#F7F8FC" />
    <corners android:radius="28dp" />
    <stroke android:width="1dp" android:color="#E2E8F0" />
    <padding android:left="8dp" android:top="8dp" android:right="8dp" android:bottom="8dp" />
</shape>
''',
    )
    write(
        commons / "commons/src/main/res/drawable/goreecloud_gallery_popup_bg_dark.xml",
        '''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#171C29" />
    <corners android:radius="28dp" />
    <stroke android:width="1dp" android:color="#334155" />
    <padding android:left="8dp" android:top="8dp" android:right="8dp" android:bottom="8dp" />
</shape>
''',
    )


def patch_search_toolbar(commons: Path) -> None:
    kotlin = commons / "commons/src/main/kotlin/org/fossify/commons/views/MySearchMenu.kt"
    replace_once(
        kotlin,
        "import android.view.LayoutInflater\nimport com.google.android.material.appbar.MaterialToolbar\n",
        "import android.view.LayoutInflater\nimport androidx.core.graphics.ColorUtils\nimport com.google.android.material.appbar.MaterialToolbar\n",
        "ColorUtils import",
    )

    replace_once(
        kotlin,
        '''    override val toolbar: MaterialToolbar?\n        get() = binding.topToolbar\n\n    fun setupMenu() {\n''',
        '''    override val toolbar: MaterialToolbar?\n        get() = binding.topToolbar\n\n    init {\n        updatePopupTheme()\n    }\n\n    private fun updatePopupTheme() {\n        val backgroundColor = context.getProperBackgroundColor()\n        val lightSurface = ColorUtils.calculateLuminance(backgroundColor) >= 0.5\n        binding.topToolbar.popupTheme = if (lightSurface) {\n            R.style.GoreeCloudGalleryPopupThemeLight\n        } else {\n            R.style.GoreeCloudGalleryPopupThemeDark\n        }\n    }\n\n    fun setupMenu() {\n''',
        "direct popup-theme initialization",
    )

    replace_once(
        kotlin,
        '''    fun updateColors() {\n        val backgroundColor = context.getProperBackgroundColor()\n''',
        '''    fun updateColors() {\n        updatePopupTheme()\n        val backgroundColor = context.getProperBackgroundColor()\n''',
        "popup-theme refresh",
    )

    layout = commons / "commons/src/main/res/layout/menu_search.xml"
    replace_once(
        layout,
        '''                android:layout_marginEnd="@dimen/small_margin"\n                app:titleTextAppearance="@style/AppTheme.ActionBar.TitleTextStyle" />\n''',
        '''                android:layout_marginEnd="@dimen/small_margin"\n                app:popupTheme="@style/GoreeCloudGalleryPopupThemeLight"\n                app:titleTextAppearance="@style/AppTheme.ActionBar.TitleTextStyle" />\n''',
        "search-toolbar popupTheme",
    )


def update_notice(gallery: Path) -> None:
    text = f'''GoreeCloud Gallery {VERSION_NAME}\n\ngc.7 is a real-device overflow-menu correction. Standard dialogs and the gc.6 no-square-thumbnail policy remain in place. The three-dot menu used by the folder and media search bars is now owned directly by the embedded Fossify Commons MySearchMenu MaterialToolbar rather than depending on inherited application overflow styles.\n\nThe toolbar receives an explicit GoreeCloud popup theme during view construction, before its menu is inflated, and receives the theme again whenever search-bar colors refresh. Light surfaces use a light rounded Glaze popup with explicit dark text. Dark surfaces use a dark rounded Glaze popup with explicit light text. This closes the dark-background/dark-text failure observed during gc.6 real-device acceptance.\n\nSquare thumbnail presentation remains unavailable: media square cropping is disabled, file thumbnails are always rounded, and folder thumbnails are always rounded. The app remains offline-first and adds no Internet permission, analytics, advertising, cloud account, or remote API.\n\nGoreeCloud Gallery remains based on Fossify Gallery 1.13.1 and Fossify Commons 6.1.5. Upstream copyright, source history, GNU GPL licensing, and third-party notices remain applicable.\n'''
    write(gallery / "GOREECLOUD-NOTICE.md", text)
    write(gallery / "app/src/main/assets/goreecloud_notice.txt", text)


def validate(gallery: Path, commons: Path) -> None:
    search_menu = read(commons / "commons/src/main/kotlin/org/fossify/commons/views/MySearchMenu.kt")
    layout = read(commons / "commons/src/main/res/layout/menu_search.xml")
    styles = read(commons / "commons/src/main/res/values/goreecloud_gallery_popup.xml")
    config = read(gallery / "app/src/main/kotlin/org/fossify/gallery/helpers/Config.kt")
    settings_xml = read(gallery / "app/src/main/res/layout/activity_settings.xml")
    file_xml = read(gallery / "app/src/main/res/layout/dialog_change_file_thumbnail_style.xml")
    folder_xml = read(gallery / "app/src/main/res/layout/dialog_change_folder_thumbnail_style.xml")

    required = (
        "ColorUtils.calculateLuminance(backgroundColor) >= 0.5",
        "R.style.GoreeCloudGalleryPopupThemeLight",
        "R.style.GoreeCloudGalleryPopupThemeDark",
        "updatePopupTheme()",
    )
    for value in required:
        if value not in search_menu:
            fail(f"missing direct toolbar invariant: {value}")

    if 'app:popupTheme="@style/GoreeCloudGalleryPopupThemeLight"' not in layout:
        fail("MaterialToolbar does not have a construction-time GoreeCloud popup theme")

    for value in (
        'name="GoreeCloudGalleryPopupThemeLight"',
        'name="GoreeCloudGalleryPopupThemeDark"',
        'name="GoreeCloudGalleryPopupMenuLight"',
        'name="GoreeCloudGalleryPopupMenuDark"',
        '<item name="android:actionOverflowMenuStyle">@style/GoreeCloudGalleryPopupMenuLight</item>',
        '<item name="android:actionOverflowMenuStyle">@style/GoreeCloudGalleryPopupMenuDark</item>',
        '<item name="android:itemTextAppearance">@style/GoreeCloudGalleryPopupTextLight</item>',
        '<item name="android:itemTextAppearance">@style/GoreeCloudGalleryPopupTextDark</item>',
    ):
        if value not in styles:
            fail(f"missing popup resource invariant: {value}")

    # Preserve gc.6's no-square contract while changing only the overflow path.
    for value in ("get() = false", "get() = FOLDER_STYLE_ROUNDED_CORNERS", "get() = true"):
        if value not in config:
            fail(f"gc.6 thumbnail invariant missing: {value}")
    for forbidden, content in (
        ("settings_crop_thumbnails", settings_xml),
        ("dialog_file_style_rounded_corners", file_xml),
        ("dialog_radio_folder_square", folder_xml),
        ("dialog_radio_folder_rounded_corners", folder_xml),
    ):
        if forbidden in content:
            fail(f"square thumbnail control returned: {forbidden}")


def main() -> None:
    if len(sys.argv) != 3:
        fail("usage: build_goreecloud_gallery_gc7.py <gallery-root> <commons-root>")

    gallery = Path(sys.argv[1]).resolve()
    commons = Path(sys.argv[2]).resolve()
    verify(gallery, GALLERY_COMMIT, "Gallery")
    verify(commons, COMMONS_COMMIT, "Commons")

    update_version(gallery)
    add_popup_resources(commons)
    patch_search_toolbar(commons)
    update_notice(gallery)
    validate(gallery, commons)

    print(f"Applied GoreeCloud Gallery {VERSION_NAME} direct toolbar overflow correction.")


if __name__ == "__main__":
    main()
