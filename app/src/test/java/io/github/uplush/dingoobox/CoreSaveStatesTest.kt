package io.github.uplush.dingoobox

import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CoreSaveStatesTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun newStatesDoNotImportOrOverwriteLegacySlots() {
        val root = temporaryFolder.newFolder("savestates")
        val key = "a".repeat(64)
        val legacy = File(root, key).apply { mkdirs() }
        val slots = listOf("auto", "quick", "slot1", "slot2", "slot3", "slot4", "slot5")
        slots.forEach { File(legacy, "$it.state").writeText("legacy-$it") }
        val current = CoreSaveStates.gameDirectory(root, key).apply { mkdirs() }
        assertNotEquals(legacy, current)
        assertEquals(File(root, CoreSaveStates.GENERATION), current.parentFile)
        assertTrue(current.listFiles().orEmpty().isEmpty())
        slots.forEach { File(current, "$it.state").writeText("upstream-$it") }
        // Auto-save cleanup and manual deletion must only touch this generation.
        slots.forEach { assertTrue(File(current, "$it.state").delete()) }
        slots.forEach { assertEquals("legacy-$it", File(legacy, "$it.state").readText()) }
        assertTrue(CoreSaveStates.hasLegacyStates(root))
    }

    @Test fun currentStatesDoNotTriggerLegacyNotice() {
        val root = temporaryFolder.newFolder("savestates")
        val current = CoreSaveStates.gameDirectory(root, "b".repeat(64)).apply { mkdirs() }
        File(current, "auto.state").writeText("new")
        assertFalse(CoreSaveStates.hasLegacyStates(root))
    }

    @Test fun preUnifiedStatesAreDetectedAndPreserved() {
        val root = temporaryFolder.newFolder("savestates")
        val olderRoot = temporaryFolder.newFolder("states")
        val oldFile = File(File(olderRoot, "old-id").apply { mkdirs() }, "slot1.state")
        oldFile.writeText("old")
        assertTrue(CoreSaveStates.hasLegacyStates(root, listOf(olderRoot)))
        assertEquals("old", oldFile.readText())
        assertTrue(root.listFiles().orEmpty().isEmpty())
    }

    @Test fun emptyOrPreviewOnlyDirectoriesDoNotTriggerNotice() {
        val root = temporaryFolder.newFolder("savestates")
        assertFalse(CoreSaveStates.hasLegacyStates(root))
        File(File(root, "old-id").apply { mkdirs() }, "auto.png").writeText("preview")
        assertFalse(CoreSaveStates.hasLegacyStates(root))
    }
}
