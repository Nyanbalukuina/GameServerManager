package gameservermanager.palworld.application

import gameservermanager.shared.construction.SteamCmdInstallCommand
import gameservermanager.shared.steamcmd.SteamCmdProcessRunner
import gameservermanager.shared.preflight.ManagedPathPolicy
import gameservermanager.palworld.domain.PalworldInstallationReport
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path

@Service
class InstallPalworldServer(
    private val managedPathPolicy: ManagedPathPolicy,
    private val processRunner: SteamCmdProcessRunner,
) {
    fun execute(command: Command): PalworldInstallationReport {
        require(managedPathPolicy.isToolPathAllowed(command.steamCmdPath)) {
            "SteamCMD保存先が管理範囲外です"
        }
        require(managedPathPolicy.isServerPathAllowed(command.installPath)) {
            "インストール先が管理範囲外です"
        }

        val steamCmdPath = Path.of(command.steamCmdPath).toAbsolutePath().normalize()
        val installPath = Path.of(command.installPath).toAbsolutePath().normalize()
        val steamCmdExecutable = steamCmdPath.resolve("steamcmd.exe")
        require(Files.isRegularFile(steamCmdExecutable)) {
            "steamcmd.exeが見つかりません。先にSteamCMDを準備してください"
        }

        Files.createDirectories(installPath)
        val installCommand = SteamCmdInstallCommand(
            steamCmdPath = steamCmdPath.toString(),
            installPath = installPath.toString(),
            appId = PALWORLD_SERVER_APP_ID,
        )
        val logPath = installPath.resolve("steamcmd-install.log")
        var result = processRunner.run(steamCmdExecutable, installCommand.arguments(), logPath)
        if (result.exitCode == STEAMCMD_FIRST_LAUNCH_EXIT_CODE) {
            result = processRunner.run(steamCmdExecutable, installCommand.arguments(), logPath)
        }
        check(result.exitCode == 0) {
            "SteamCMDが終了コード${result.exitCode}で失敗しました。ログ: ${result.logPath}"
        }

        val gameExecutable = installPath.resolve("PalServer.exe")
        check(Files.isRegularFile(gameExecutable)) {
            "SteamCMDは正常終了しましたがPalServer.exeが見つかりません。ログ: ${result.logPath}"
        }

        return PalworldInstallationReport(
            completed = true,
            appId = PALWORLD_SERVER_APP_ID,
            installPath = installPath.toString(),
            executablePath = gameExecutable.toString(),
            logPath = result.logPath,
            exitCode = result.exitCode,
        )
    }

    data class Command(
        val steamCmdPath: String,
        val installPath: String,
    )

    companion object {
        const val PALWORLD_SERVER_APP_ID = 2394010
        private const val STEAMCMD_FIRST_LAUNCH_EXIT_CODE = 7
    }
}
