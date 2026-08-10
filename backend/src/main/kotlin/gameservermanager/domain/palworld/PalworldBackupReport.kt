package gameservermanager.domain.palworld

data class PalworldBackupReport(
    val completed: Boolean,
    val backupPath: String,
    val sizeBytes: Long,
    val fileCount: Int,
)
