package gameservermanager.application.palworld

import gameservermanager.application.server.GameServerRegistrationStore
import gameservermanager.application.steamcmd.PrepareSteamCmd
import gameservermanager.domain.construction.ConstructionStep
import gameservermanager.domain.construction.ConstructionStepStatus
import gameservermanager.domain.construction.ServerConstructionReport
import gameservermanager.domain.server.GameServerRegistration
import gameservermanager.web.error.GameServerAlreadyExistsException
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock

@Service
class ConstructPalworldServer(
    private val prepareSteamCmd: PrepareSteamCmd,
    private val installPalworldServer: InstallPalworldServer,
    private val configurePalworldServer: ConfigurePalworldServer,
    private val configurePalworldAutomation: ConfigurePalworldAutomation,
    private val startPalworldServer: StartPalworldServer,
    private val registrationStore: GameServerRegistrationStore,
) {
    @Synchronized
    fun execute(command: Command): ServerConstructionReport {
        if (registrationStore.findByGame(GAME) != null) {
            throw GameServerAlreadyExistsException(GAME)
        }

        val steps = mutableListOf<ConstructionStep>()
        val steamCmdExecutable = Path.of(command.steamCmdPath)
            .toAbsolutePath()
            .normalize()
            .resolve("steamcmd.exe")
        if (Files.isRegularFile(steamCmdExecutable)) {
            steps += completed("steamcmd", "SteamCMDの準備", "既存のSteamCMDを使用します")
        } else {
            prepareSteamCmd.execute(PrepareSteamCmd.Command(command.steamCmdPath))
            steps += completed("steamcmd", "SteamCMDの準備", "SteamCMDをダウンロードして配置しました")
        }

        installPalworldServer.execute(
            InstallPalworldServer.Command(
                steamCmdPath = command.steamCmdPath,
                installPath = command.installPath,
            ),
        )
        steps += completed("install", "Palworldサーバーのインストール", "SteamCMDによるインストールが完了しました")

        configurePalworldServer.execute(
            ConfigurePalworldServer.Command(
                serverName = command.serverName,
                installPath = command.installPath,
                gamePort = command.gamePort,
                rconPort = command.rconPort,
                maxPlayers = command.maxPlayers,
                serverPassword = command.serverPassword,
                adminPassword = command.adminPassword,
            ),
        )
        steps += completed("configuration", "Palworld設定の保存", "サーバー設定を保存しました")

        configurePalworldAutomation.save(
            ConfigurePalworldAutomation.Command(
                enabled = command.automationEnabled,
                shutdownTime = command.shutdownTime,
                startupTime = command.startupTime,
                backupAfterShutdown = command.backupAfterShutdown,
                gamePort = command.gamePort,
                maxPlayers = command.maxPlayers,
            ),
        )
        steps += completed("automation", "自動運転設定の保存", "停止・バックアップ・起動設定を保存しました")

        startPalworldServer.execute(
            StartPalworldServer.Command(
                installPath = command.installPath,
                gamePort = command.gamePort,
                maxPlayers = command.maxPlayers,
            ),
        )
        steps += completed("start", "Palworldサーバーの起動", "ゲームポートの待受を確認しました")

        registrationStore.create(
            GameServerRegistration(
                game = GAME,
                serverId = "palworld-main",
                mode = "REAL",
                state = "RUNNING",
                serverName = command.serverName,
                installPath = command.installPath,
                workspacePath = "",
                gamePort = command.gamePort,
                rconPort = command.rconPort,
                createdAt = Clock.systemUTC().instant(),
            ),
        )
        steps += completed("registration", "管理対象への登録", "Palworldサーバーを登録しました")

        return ServerConstructionReport(
            completed = true,
            mode = "REAL",
            installPath = command.installPath,
            steps = steps,
        )
    }

    private fun completed(id: String, label: String, message: String): ConstructionStep {
        return ConstructionStep(id, label, ConstructionStepStatus.COMPLETED, message)
    }

    data class Command(
        val serverName: String,
        val installPath: String,
        val steamCmdPath: String,
        val gamePort: Int,
        val rconPort: Int,
        val maxPlayers: Int,
        val serverPassword: String,
        val adminPassword: String,
        val automationEnabled: Boolean,
        val shutdownTime: String,
        val startupTime: String,
        val backupAfterShutdown: Boolean,
    )

    companion object {
        private const val GAME = "PALWORLD"
    }
}
