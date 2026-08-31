package gameservermanager.palworld.application

import gameservermanager.shared.server.GameServerRegistrationStore
import gameservermanager.shared.construction.CreateGamePortAccess
import gameservermanager.shared.construction.GameFirewallManager
import gameservermanager.shared.construction.GameFirewallRuleCommand
import gameservermanager.shared.steamcmd.PrepareSteamCmd
import gameservermanager.shared.construction.ConstructionStep
import gameservermanager.shared.construction.ConstructionStepStatus
import gameservermanager.shared.construction.ConstructionProgressStepDefinition
import gameservermanager.shared.construction.ConstructionProgressTracker
import gameservermanager.shared.construction.ServerConstructionReport
import gameservermanager.shared.server.GameServerRegistration
import gameservermanager.shared.error.GameServerAlreadyExistsException
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
    private val progressTracker: ConstructionProgressTracker,
) {
    @Synchronized
    fun execute(command: Command): ServerConstructionReport {
        if (registrationStore.findByGame(GAME) != null) {
            throw GameServerAlreadyExistsException(GAME)
        }

        progressTracker.start(GAME, PROGRESS_STEPS)
        val gamePortAccess = createGamePortAccess.execute(command.gamePortAccess)
        val steps = mutableListOf<ConstructionStep>()
        val steamCmdExecutable = Path.of(command.steamCmdPath)
            .toAbsolutePath()
            .normalize()
            .resolve("steamcmd.exe")
        tracked("steamcmd", "SteamCMDの準備が完了しました") {
            if (Files.isRegularFile(steamCmdExecutable)) {
                steps += completed("steamcmd", "SteamCMDの準備", "既存のSteamCMDを使用します")
            } else {
                prepareSteamCmd.execute(PrepareSteamCmd.Command(command.steamCmdPath))
                steps += completed("steamcmd", "SteamCMDの準備", "SteamCMDをダウンロードして配置しました")
            }
        }

        tracked("install", "SteamCMDによるダウンロードが完了しました") {
            installPalworldServer.execute(
                InstallPalworldServer.Command(
                    steamCmdPath = command.steamCmdPath,
                    installPath = command.installPath,
                ),
            )
            steps += completed("install", "Palworldサーバーのインストール", "SteamCMDによるインストールが完了しました")
        }

        tracked("configuration", "PalWorldSettings.iniを保存しました") { configurePalworldServer.execute(
            ConfigurePalworldServer.Command(
                serverName = command.serverName,
                installPath = command.installPath,
                gamePort = command.gamePort,
                rconPort = command.rconPort,
                maxPlayers = command.maxPlayers,
                serverPassword = command.serverPassword,
                adminPassword = command.adminPassword,
                serverDescription = command.serverDescription,
                expRate = command.expRate, palCaptureRate = command.palCaptureRate,
                palSpawnRate = command.palSpawnRate, enemyDropRate = command.enemyDropRate,
                eggHatchingTime = command.eggHatchingTime, deathPenalty = command.deathPenalty,
                pvpEnabled = command.pvpEnabled, friendlyFireEnabled = command.friendlyFireEnabled,
                baseCampMaxNum = command.baseCampMaxNum, baseCampWorkerMaxNum = command.baseCampWorkerMaxNum,
            ),
        )
            steps += completed("configuration", "Palworld設定の保存", "サーバー設定を保存しました")
        }

        tracked("automation", "自動運転設定を保存しました") { configurePalworldAutomation.save(
            ConfigurePalworldAutomation.Command(
                enabled = command.automationEnabled,
                shutdownTime = command.shutdownTime,
                startupTime = command.startupTime,
                gamePort = command.gamePort,
                maxPlayers = command.maxPlayers,
            ),
        )
            steps += completed("automation", "自動運転設定の保存", "停止・起動設定を保存しました")
        }

        val firewallCommand = GameFirewallRuleCommand(
            game = GAME,
            serverId = SERVER_ID,
            gamePort = command.gamePort,
            remoteAddresses = gamePortAccess.remoteAddresses(),
        )
        tracked("firewall", "ゲームポートの受信規則を追加しました") {
            val firewallRuleName = gameFirewallManager.apply(firewallCommand)
            steps += completed(
                "firewall",
                "Windows Firewallの設定",
                "ゲームポートの受信規則を追加しました: $firewallRuleName",
            )
        }

        try {
            tracked("start", "ゲームポートの待受を確認しました") { startPalworldServer.execute(
                StartPalworldServer.Command(
                    installPath = command.installPath,
                    gamePort = command.gamePort,
                    maxPlayers = command.maxPlayers,
                ),
            )
                steps += completed("start", "Palworldサーバーの起動", "ゲームポートの待受を確認しました")
            }

            tracked("registration", "Palworldサーバーを管理対象へ登録しました") { registrationStore.create(
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
            }
        } catch (exception: Exception) {
            try {
                gameFirewallManager.remove(firewallCommand)
            } catch (rollbackException: Exception) {
                exception.addSuppressed(rollbackException)
            }
            throw exception
        }

        progressTracker.finish(GAME)
        return ServerConstructionReport(
            completed = true,
            mode = "REAL",
            installPath = command.installPath,
            steps = steps,
        )
    }

    private fun <T> tracked(id: String, successMessage: String, action: () -> T): T {
        progressTracker.running(GAME, id)
        return try {
            action().also { progressTracker.completed(GAME, id, successMessage) }
        } catch (exception: Exception) {
            progressTracker.failed(GAME, id, exception.message ?: "処理に失敗しました")
            throw exception
        }
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
        val serverDescription: String = "",
        val expRate: Double = 1.0,
        val palCaptureRate: Double = 1.0,
        val palSpawnRate: Double = 1.0,
        val enemyDropRate: Double = 1.0,
        val eggHatchingTime: Double = 2.0,
        val deathPenalty: String = "All",
        val pvpEnabled: Boolean = false,
        val friendlyFireEnabled: Boolean = false,
        val baseCampMaxNum: Int = 128,
        val baseCampWorkerMaxNum: Int = 15,
    )

    companion object {
        const val GAME = "PALWORLD"
        private const val SERVER_ID = "palworld-main"
        private val PROGRESS_STEPS = listOf(
            ConstructionProgressStepDefinition("steamcmd", "SteamCMDの準備・ダウンロード"),
            ConstructionProgressStepDefinition("install", "Palworld Dedicated Serverのダウンロード"),
            ConstructionProgressStepDefinition("configuration", "サーバー設定の保存"),
            ConstructionProgressStepDefinition("automation", "自動運転設定の保存"),
            ConstructionProgressStepDefinition("firewall", "Windows Firewallの設定"),
            ConstructionProgressStepDefinition("start", "サーバー起動とポート待受確認"),
            ConstructionProgressStepDefinition("registration", "管理対象への登録"),
        )
    }
}
