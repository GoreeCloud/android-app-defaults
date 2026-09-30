package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherFolderCodecTest {
    @Test
    fun foldersRoundTripWithNamesAndOrderedMembers() {
        val folders = listOf(
            LauncherFolder(
                id = "folder-a",
                name = "Work & Notes",
                appKeys = listOf("work:app.one", "work:app.two"),
                profileKind = LauncherDrawerProfileKind.WORK,
            ),
            LauncherFolder(
                id = "folder-b",
                name = "Media",
                appKeys = emptyList(),
            ),
        )

        assertEquals(folders, LauncherFolderCodec.decode(LauncherFolderCodec.encode(folders)))
    }

    @Test
    fun legacyThreeColumnFoldersMigrateAsUserProfile() {
        val legacy = "Zm9sZGVyLWxlZ2FjeQ\tTGVnYWN5\tdXNlcjphcHA"
        val restored = LauncherFolderCodec.decode(legacy).single()

        assertEquals("folder-legacy", restored.id)
        assertEquals("Legacy", restored.name)
        assertEquals(LauncherDrawerProfileKind.USER, restored.profileKind)
        assertEquals(listOf("user:app"), restored.appKeys)
    }

    @Test
    fun malformedRowsFailSoftWithoutInventingFolders() {
        val encoded = LauncherFolderCodec.encode(
            listOf(LauncherFolder("good", "Utilities", listOf("app"))),
        )
        val decoded = LauncherFolderCodec.decode("not\ta\tvalid\textra\n" + encoded)

        assertEquals(1, decoded.size)
        assertEquals("good", decoded.single().id)
    }

    @Test
    fun namesAreNormalizedAndBounded() {
        val normalized = LauncherFolderPolicy.normalizeName("   My   Folder   ")
        assertEquals("My Folder", normalized)
        assertTrue(
            LauncherFolderPolicy.normalizeName("x".repeat(100))!!.length <=
                LauncherFolderPolicy.MAX_NAME_LENGTH,
        )
    }
}
