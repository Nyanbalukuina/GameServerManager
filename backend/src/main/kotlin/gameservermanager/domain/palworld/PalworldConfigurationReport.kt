package gameservermanager.domain.palworld

data class PalworldConfigurationReport(
    val completed: Boolean,
    val settingsPath: String,
    val backupPath: String?,
    val configuredKeys: List<String>,
)
