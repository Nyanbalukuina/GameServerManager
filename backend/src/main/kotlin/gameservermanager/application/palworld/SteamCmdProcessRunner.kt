package gameservermanager.application.palworld

import java.nio.file.Path

interface SteamCmdProcessRunner {
    fun run(executable: Path, arguments: List<String>, logPath: Path): SteamCmdProcessResult
}

data class SteamCmdProcessResult(
    val exitCode: Int,
    val logPath: String,
)
