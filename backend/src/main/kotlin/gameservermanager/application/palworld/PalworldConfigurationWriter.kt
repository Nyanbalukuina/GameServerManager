package gameservermanager.application.palworld

import java.nio.file.Path

interface PalworldConfigurationWriter {
    fun write(command: PalworldConfigurationWriteCommand): PalworldConfigurationWriteResult
}

data class PalworldConfigurationWriteCommand(
    val defaultSettingsPath: Path,
    val settingsPath: Path,
    val backupDirectory: Path,
    val values: Map<String, String>,
)

data class PalworldConfigurationWriteResult(
    val settingsPath: String,
    val backupPath: String?,
)
