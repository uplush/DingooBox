package io.github.uplush.dingoobox

import java.io.File

/** Storage generations follow the snapshot format, not each upstream commit. */
internal object CoreSaveStates {
    const val GENERATION = "dingooemu-v4-libretro-v1"

    fun gameDirectory(statesRoot: File, gameKey: String): File =
        File(File(statesRoot, GENERATION), gameKey)

    fun hasLegacyStates(statesRoot: File, olderRoots: List<File> = emptyList()): Boolean {
        val legacyGameDirectories = statesRoot.listFiles().orEmpty()
            .filter { it.isDirectory && it.name != GENERATION }
        val olderGameDirectories = olderRoots.flatMap { root ->
            root.listFiles().orEmpty().filter(File::isDirectory)
        }
        return (legacyGameDirectories + olderGameDirectories).any { directory ->
            directory.listFiles().orEmpty().any { file ->
                file.isFile && file.extension == "state"
            }
        }
    }
}
