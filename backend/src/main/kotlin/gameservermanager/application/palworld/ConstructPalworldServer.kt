package gameservermanager.application.palworld

import gameservermanager.application.server.GameServerRegistrationStore
import gameservermanager.application.construction.CreateGamePortAccess
import gameservermanager.application.construction.GameFirewallManager
import gameservermanager.application.construction.GameFirewallRuleCommand
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
    private val createGamePortAccess: CreateGamePortAccess,
    private val gameFirewallManager: GameFirewallManager,
) {
    @Synchronized
    fun execute(command: Command): ServerConstructionReport {
        if (registrationStore.findByGame(GAME) != null) {
            throw GameServerAlreadyExistsException(GAME)
        }

        val gamePortAccess = createGamePortAccess.execute(command.gamePortAccess)
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
                gamePort = command.gamePort,
                maxPlayers = command.maxPlayers,
            ),
        )
        steps += completed("automation", "自動運転設定の保存", "停止・起動設定を保存しました")

        val firewallCommand = GameFirewallRuleCommand(
            game = GAME,
            serverId = SERVER_ID,
            gamePort = command.gamePort,
            remoteAddresses = gamePortAccess.remoteAddresses(),
        )
        val firewallRuleName = gameFirewallManager.apply(firewallCommand)
        steps += completed(
            "firewall",
            "Windows Firewallの設定",
            "ゲームポートの受信規則を追加しました: $firewallRuleName",
        )

        try {
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
                    serverId = SERVER_ID,
                    mode = "REAL",
                    state = "RUNNING",
                    serverName = command.serverName,
                    installPath = command.installPath,
                    workspacePath = "",
                    gamePort = command.gamePort,
                    rconPort = command.rconPort,
                    createdAt = Clock.systemUTC().instant(),
                    gamePortAccess = gamePortAccess,
                ),
            )
            steps += completed("registration", "管理対象への登録", "Palworldサーバーを登録しました")
        } catch (exception: RuntimeException) {
            try {
                gameFirewallManager.remove(firewallCommand)
            } catch (rollbackException: RuntimeException) {
                exception.addSuppressed(rollbackException)
            }
            throw exception
        }

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
        val gamePortAccess: CreateGamePortAccess.Command,
    )

    companion object {
        private const val GAME = "PALWORLD"
        private const val SERVER_ID = "palworld-main"
    }
}
