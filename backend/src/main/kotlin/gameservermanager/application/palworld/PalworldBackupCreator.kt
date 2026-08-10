package gameservermanager.application.palworld

import java.nio.file.Path

interface PalworldBackupCreator {
    fun create(source: Path, backupDirectory: Path): PalworldBackupResult
}

data class PalworldBackupResult(
    val backupPath: String,
    val sizeBytes: Long,
    val fileCount: Int,
)
