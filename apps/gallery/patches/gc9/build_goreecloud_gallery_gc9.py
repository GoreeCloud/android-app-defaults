#!/usr/bin/env python3
"""Apply GoreeCloud Gallery gc.9 Stable-candidate package identity."""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

GALLERY_COMMIT = "b28299dc33821eee8d108a9880ce87876cf31443"
COMMONS_COMMIT = "acfd352df1a1852d17a5f77def8b7ad6e522a5b6"
VERSION_NAME = "1.0.0"
VERSION_CODE = "10009"


def fail(message: str) -> None:
    raise SystemExit(f"gc.9 patch failed: {message}")


def read(path: Path) -> str:
    if not path.is_file():
        fail(f"missing {path}")
    return path.read_text(encoding="utf-8")


def write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


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


def update_notice(gallery: Path) -> None:
    notice = f'''GoreeCloud Gallery {VERSION_NAME}\n\nThis package is the GoreeCloud Gallery 1.0.0 Stable-candidate binary identity. The semantic version is final, but Stable classification is not automatic: this exact binary remains an Acceptance Candidate or Signed Release Candidate until the long-lived signer, protected repository controls, representative-device acceptance, accessibility, upgrade/recovery, Glaze UI review, licensing, and final evidence gates are complete.\n\nThe application remains offline-first and does not add analytics, advertising, tracking, cloud accounts, remote APIs, or android.permission.INTERNET. Android OS user/profile isolation, the Android application sandbox, and platform-authorized media/storage access remain the application-appropriate private-data boundary.\n\nGlaze UI remains mandatory for GoreeCloud-controlled surfaces. Rounded thumbnail behavior, readable light/dark toolbar overflow presentation, accessibility requirements, and the gc.8 GoreeCloud-owned behavioral policy tests are preserved unchanged.\n\nGoreeCloud Gallery remains based on Fossify Gallery 1.13.1 and Fossify Commons 6.1.5. Upstream copyright, source history, GNU GPL licensing, and third-party notices remain applicable.\n'''
    write(gallery / "GOREECLOUD-NOTICE.md", notice)
    write(gallery / "app/src/main/assets/goreecloud_notice.txt", notice)


def validate(gallery: Path) -> None:
    properties = read(gallery / "gradle.properties")
    policy = read(gallery / "app/src/main/kotlin/org/fossify/gallery/helpers/GoreeCloudGalleryPolicy.kt")
    tests = read(gallery / "app/src/test/kotlin/org/fossify/gallery/helpers/GoreeCloudGalleryPolicyTest.kt")

    for required in (f"VERSION_NAME={VERSION_NAME}", f"VERSION_CODE={VERSION_CODE}"):
        if required not in properties:
            fail(f"missing version invariant: {required}")

    for required in (
        "CROP_THUMBNAILS = false",
        "FILE_ROUNDED_CORNERS = true",
        "FOLDER_STYLE = FOLDER_STYLE_ROUNDED_CORNERS",
    ):
        if required not in policy:
            fail(f"gc.8 policy invariant missing: {required}")

    for required in (
        "fun thumbnailsRemainUncropped()",
        "fun fileThumbnailsRemainRounded()",
        "fun folderThumbnailsRemainRounded()",
    ):
        if required not in tests:
            fail(f"gc.8 behavioral test missing: {required}")


def main() -> None:
    if len(sys.argv) != 3:
        fail("usage: build_goreecloud_gallery_gc9.py <gallery-root> <commons-root>")

    gallery = Path(sys.argv[1]).resolve()
    commons = Path(sys.argv[2]).resolve()
    verify(gallery, GALLERY_COMMIT, "Gallery")
    verify(commons, COMMONS_COMMIT, "Commons")
    update_version(gallery)
    update_notice(gallery)
    validate(gallery)
    print(f"Applied GoreeCloud Gallery gc.9 Stable-candidate identity: {VERSION_NAME} ({VERSION_CODE}).")


if __name__ == "__main__":
    main()
