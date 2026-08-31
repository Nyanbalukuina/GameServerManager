package gameservermanager.palworld.domain

data class PalworldInstallationReport(
    val completed: Boolean,
    val appId: Int,
    val installPath: String,
    val executablePath: String,
    val logPath: String,
    val exitCode: Int,
)
