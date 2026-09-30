#!/usr/bin/env python3
"""Apply GoreeCloud Gallery gc.8 behavioral-test foundation."""
from __future__ import annotations
import re, subprocess, sys
from pathlib import Path

GALLERY_COMMIT = "b28299dc33821eee8d108a9880ce87876cf31443"
COMMONS_COMMIT = "acfd352df1a1852d17a5f77def8b7ad6e522a5b6"
VERSION_NAME, VERSION_CODE = "1.0.0-gc.7", "10007"

def fail(msg): raise SystemExit(f"gc.8 patch failed: {msg}")
def read(p):
    if not p.is_file(): fail(f"missing {p}")
    return p.read_text(encoding="utf-8")
def write(p, s): p.parent.mkdir(parents=True, exist_ok=True); p.write_text(s, encoding="utf-8")
def replace(p, old, new, label):
    s = read(p)
    if s.count(old) != 1: fail(f"expected one {label} match in {p}")
    write(p, s.replace(old, new, 1))
def verify(root, sha, label):
    got = subprocess.check_output(["git", "-C", str(root), "rev-parse", "HEAD"], text=True).strip()
    if got != sha: fail(f"{label} checkout is {got}, expected {sha}")

def version(g):
    p = g / "gradle.properties"; s = read(p)
    s, a = re.subn(r"^VERSION_NAME=.*$", f"VERSION_NAME={VERSION_NAME}", s, count=1, flags=re.M)
    s, b = re.subn(r"^VERSION_CODE=.*$", f"VERSION_CODE={VERSION_CODE}", s, count=1, flags=re.M)
    if a != 1 or b != 1: fail("version metadata")
    write(p, s)

def add_policy(g):
    policy = g / "app/src/main/kotlin/org/fossify/gallery/helpers/GoreeCloudGalleryPolicy.kt"
    write(policy, """package org.fossify.gallery.helpers

/**
 * Pure GoreeCloud-owned presentation policy.
 *
 * Keeping these invariants outside Android framework state makes them directly testable and
 * prevents preference drift from re-enabling presentation choices removed by GoreeCloud.
 */
object GoreeCloudGalleryPolicy {
    const val CROP_THUMBNAILS = false
    const val FILE_ROUNDED_CORNERS = true
    const val FOLDER_STYLE = FOLDER_STYLE_ROUNDED_CORNERS
}
""")
    config = g / "app/src/main/kotlin/org/fossify/gallery/helpers/Config.kt"
    replace(config, """    var cropThumbnails: Boolean
        get() = false
        set(cropThumbnails) = prefs.edit().putBoolean(CROP_THUMBNAILS, false).apply()
""", """    var cropThumbnails: Boolean
        get() = GoreeCloudGalleryPolicy.CROP_THUMBNAILS
        set(cropThumbnails) = prefs.edit().putBoolean(CROP_THUMBNAILS, GoreeCloudGalleryPolicy.CROP_THUMBNAILS).apply()
""", "crop policy")
    replace(config, """    var folderStyle: Int
        get() = FOLDER_STYLE_ROUNDED_CORNERS
        set(folderStyle) = prefs.edit().putInt(FOLDER_THUMBNAIL_STYLE, FOLDER_STYLE_ROUNDED_CORNERS).apply()
""", """    var folderStyle: Int
        get() = GoreeCloudGalleryPolicy.FOLDER_STYLE
        set(folderStyle) = prefs.edit().putInt(FOLDER_THUMBNAIL_STYLE, GoreeCloudGalleryPolicy.FOLDER_STYLE).apply()
""", "folder policy")
    replace(config, """    var fileRoundedCorners: Boolean
        get() = true
        set(fileRoundedCorners) = prefs.edit().putBoolean(FILE_ROUNDED_CORNERS, true).apply()
""", """    var fileRoundedCorners: Boolean
        get() = GoreeCloudGalleryPolicy.FILE_ROUNDED_CORNERS
        set(fileRoundedCorners) = prefs.edit().putBoolean(FILE_ROUNDED_CORNERS, GoreeCloudGalleryPolicy.FILE_ROUNDED_CORNERS).apply()
""", "file policy")

def add_tests(g):
    build = g / "app/build.gradle.kts"
    replace(build, """dependencies {
    implementation(libs.fossify.commons)
""", """dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation(libs.fossify.commons)
""", "JUnit dependency")
    test = g / "app/src/test/kotlin/org/fossify/gallery/helpers/GoreeCloudGalleryPolicyTest.kt"
    write(test, """package org.fossify.gallery.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoreeCloudGalleryPolicyTest {
    @Test
    fun thumbnailsRemainUncropped() {
        assertFalse(GoreeCloudGalleryPolicy.CROP_THUMBNAILS)
    }

    @Test
    fun fileThumbnailsRemainRounded() {
        assertTrue(GoreeCloudGalleryPolicy.FILE_ROUNDED_CORNERS)
    }

    @Test
    fun folderThumbnailsRemainRounded() {
        assertEquals(FOLDER_STYLE_ROUNDED_CORNERS, GoreeCloudGalleryPolicy.FOLDER_STYLE)
    }
}
""")

def main():
    if len(sys.argv) != 3: fail("usage: patch.py <gallery> <commons>")
    g, c = Path(sys.argv[1]), Path(sys.argv[2])
    verify(g, GALLERY_COMMIT, "Gallery"); verify(c, COMMONS_COMMIT, "Commons")
    version(g); add_policy(g); add_tests(g)
    print("Applied GoreeCloud Gallery gc.8 behavioral-test foundation.")

if __name__ == "__main__":
    main()
