package gameservermanager.application.asa

import gameservermanager.application.construction.SteamCmdInstallCommand
import gameservermanager.application.preflight.ManagedPathPolicy
import gameservermanager.application.steamcmd.SteamCmdProcessRunner
import gameservermanager.domain.asa.AsaInstallationReport
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path

@Service
class InstallAsaServer(
    private val managedPathPolicy: ManagedPathPolicy,
    private val processRunner: SteamCmdProcessRunner,
) {
    fun execute(command: Command): AsaInstallationReport {
        require(managedPathPolicy.isToolPathAllowed(command.steamCmdPath)) {
            "SteamCMD保存先が管理範囲外です"
        }
        require(managedPathPolicy.isServerPathAllowed(command.installPath)) {
            "ASAインストール先が管理範囲外です"
        }

        val steamCmdPath = Path.of(command.steamCmdPath)
            .toAbsolutePath()
            .normalize()
        val installPath = Path.of(command.installPath)
            .toAbsolutePath()
            .normalize()
        val steamCmdExecutable = steamCmdPath.resolve("steamcmd.exe")

        require(Files.isRegularFile(steamCmdExecutable)) {
            "steamcmd.exeが見つかりません。先にSteamCMDを準備してください"
        }

        Files.createDirectories(installPath)

        // ASAのApp IDを使用してSteamCMDの引数を作成する。
        val installCommand = SteamCmdInstallCommand(
            steamCmdPath = steamCmdPath.toString(),
            installPath = installPath.toString(),
            appId = ASA_SERVER_APP_ID,
        )

        val logPath = installPath.resolve("steamcmd-install.log")
        var result = processRunner.run(
            steamCmdExecutable,
            installCommand.arguments(),
            logPath,
        )

        // SteamCMDの初回準備による終了コード7の場合は、もう一度実行する。
        if (result.exitCode == STEAMCMD_FIRST_LAUNCH_EXIT_CODE) {
            result = processRunner.run(
                steamCmdExecutable,
                installCommand.arguments(),
                logPath,
            )
        }

        check(result.exitCode == 0) {
            "SteamCMDが終了コード${result.exitCode}で失敗しました。ログ: ${result.logPath}"
        }

        val gameExecutable = installPath
            .resolve("ShooterGame")
            .resolve("Binaries")
            .resolve("Win64")
            .resolve("ArkAscendedServer.exe")

        check(Files.isRegularFile(gameExecutable)) {
            "SteamCMDは正常終了しましたがArkAscendedServer.exeが見つかりません。ログ: ${result.logPath}"
        }

        return AsaInstallationReport(
            completed = true,
            appId = ASA_SERVER_APP_ID,
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
        const val ASA_SERVER_APP_ID = 2430930
        private const val STEAMCMD_FIRST_LAUNCH_EXIT_CODE = 7
    }
}