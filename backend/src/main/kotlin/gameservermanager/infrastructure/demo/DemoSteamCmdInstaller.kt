package gameservermanager.infrastructure.demo

import gameservermanager.application.construction.SteamCmdInstallCommand
import gameservermanager.application.construction.SteamCmdInstallResult
import gameservermanager.application.construction.SteamCmdInstaller
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class DemoSteamCmdInstaller() : SteamCmdInstaller {
    private var basePath: Path? = null

    constructor(basePath: Path) : this() {
        this.basePath = basePath
    }

    override fun install(command: SteamCmdInstallCommand): SteamCmdInstallResult {
        val workspaceRoot = basePath ?: Path.of(System.getProperty("java.io.tmpdir"))
        val workspace = workspaceRoot.resolve("GameServerManagerDemo")
        val steamCmdDirectory = workspace.resolve("tools").resolve("steamcmd")
        val runtimeDirectory = workspace.resolve("servers").resolve("palworld").resolve("main").resolve("runtime")
        val savedDirectory = runtimeDirectory.resolve("Pal").resolve("Saved")

        Files.createDirectories(steamCmdDirectory)
        Files.createDirectories(savedDirectory.resolve("Config").resolve("WindowsServer"))
        Files.createDirectories(savedDirectory.resolve("SaveGames"))
        Files.createDirectories(savedDirectory.resolve("Logs"))
        Files.createDirectories(workspace.resolve("backups").resolve("palworld-main"))
        Files.createDirectories(workspace.resolve("logs"))

        val steamCmdExecutable = steamCmdDirectory.resolve("steamcmd.exe")
        val gameExecutable = runtimeDirectory.resolve("PalServer.exe")
        Files.writeString(steamCmdExecutable, "demo steamcmd")
        Files.writeString(gameExecutable, "demo palworld server")
        Files.writeString(workspace.resolve("steamcmd-arguments.txt"), command.arguments().joinToString(System.lineSeparator()))

        return SteamCmdInstallResult(
            workspacePath = workspace.toString(),
            steamCmdExecutablePath = steamCmdExecutable.toString(),
            gameExecutablePath = gameExecutable.toString(),
        )
    }
}
