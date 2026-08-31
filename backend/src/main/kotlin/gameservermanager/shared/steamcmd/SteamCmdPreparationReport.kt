package gameservermanager.shared.steamcmd

data class SteamCmdPreparationReport(
    val completed: Boolean,
    val sourceUrl: String,
    val installPath: String,
    val executablePath: String,
    val downloadedBytes: Long,
)
