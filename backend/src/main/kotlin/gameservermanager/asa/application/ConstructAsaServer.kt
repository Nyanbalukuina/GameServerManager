package gameservermanager.asa.application

import gameservermanager.shared.construction.CreateGamePortAccess
import gameservermanager.shared.construction.GameFirewallManager
import gameservermanager.shared.construction.GameFirewallRuleCommand
import gameservermanager.shared.server.GameServerRegistrationStore
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

// SteamCMD準備から起動・登録までASAの実構築を一括実行する。
@Service
class ConstructAsaServer(
    private val prepareSteamCmd: PrepareSteamCmd,
    private val installAsaServer: InstallAsaServer,
    private val configureAsaServer: ConfigureAsaServer,
    private val startAsaServer: StartAsaServer,
    private val registrationStore: GameServerRegistrationStore,
    private val createGamePortAccess: CreateGamePortAccess,
    private val firewallManager: GameFirewallManager,
    private val progressTracker: ConstructionProgressTracker,
) {
    @Synchronized
    fun execute(command: Command): ServerConstructionReport {
        if (registrationStore.findByGame(GAME) != null) throw GameServerAlreadyExistsException(GAME)
        require(command.rconPort !in setOf(command.gamePort, command.gamePort + 1, command.queryPort)) {
            "RCONポートはゲーム・Peer・Queryポートと分けてください"
        }

        progressTracker.start(GAME, PROGRESS_STEPS)
        val access = createGamePortAccess.execute(command.gamePortAccess)
        val steps = mutableListOf<ConstructionStep>()
        val steamCmdExecutable = Path.of(command.steamCmdPath).toAbsolutePath().normalize().resolve("steamcmd.exe")
        tracked("steamcmd", "SteamCMDの準備が完了しました") {
            if (Files.isRegularFile(steamCmdExecutable)) {
                steps += completed("steamcmd", "SteamCMDの準備", "既存のSteamCMDを使用します")
            } else {
                prepareSteamCmd.execute(PrepareSteamCmd.Command(command.steamCmdPath))
                steps += completed("steamcmd", "SteamCMDの準備", "SteamCMDをダウンロードして配置しました")
            }
        }

        tracked("install", "SteamCMDによるダウンロードが完了しました") {
            installAsaServer.execute(InstallAsaServer.Command(command.steamCmdPath, command.installPath))
            steps += completed("install", "ASAサーバーのインストール", "SteamCMDによるインストールが完了しました")
        }

        tracked("configuration", "GameUserSettings.iniとGame.iniを保存しました") {
            configureAsaServer.execute(
                ConfigureAsaServer.Command(
                    command.serverName, command.installPath, command.rconPort,
                    command.serverPassword, command.adminPassword,
                    command.pveEnabled, command.xpMultiplier,
                    command.tamingSpeedMultiplier, command.harvestAmountMultiplier,
                    command.eggHatchSpeedMultiplier, command.babyMatureSpeedMultiplier,
                ),
            )
            steps += completed("configuration", "ASA設定の保存", "GameUserSettings.iniを保存しました")
        }

        val firewallCommands = listOf(command.gamePort, command.gamePort + 1, command.queryPort).map { port ->
            GameFirewallRuleCommand(GAME, SERVER_ID, port, access.remoteAddresses())
        }
        val applied = mutableListOf<GameFirewallRuleCommand>()
        try {
            tracked("firewall", "ASA用UDP受信規則を追加しました") {
                firewallCommands.forEach {
                    firewallManager.apply(it)
                    applied += it
                }
                steps += completed("firewall", "Windows Firewallの設定", "ASA用UDP受信規則を追加しました")
            }

            tracked("start", "ゲームポートの待受を確認しました") {
                startAsaServer.execute(
                    StartAsaServer.Command(
                        command.installPath, command.map, command.gamePort, command.queryPort, command.maxPlayers,
                    ),
                )
                steps += completed("start", "ASAサーバーの起動", "ゲームポートの待受を確認しました")
            }

            tracked("registration", "ASAサーバーを管理対象へ登録しました") {
                registrationStore.create(
                    GameServerRegistration(
                        GAME, SERVER_ID, "REAL", "RUNNING", command.serverName, command.installPath, "",
                        command.gamePort, command.rconPort, Clock.systemUTC().instant(), access,
                        command.gamePort + 1, command.queryPort, command.map, command.maxPlayers,
                    ),
                )
                steps += completed("registration", "管理対象への登録", "ASAサーバーを登録しました")
            }
        } catch (exception: Exception) {
            applied.asReversed().forEach {
                try { firewallManager.remove(it) } catch (rollback: Exception) { exception.addSuppressed(rollback) }
            }
            throw exception
        }

        progressTracker.finish(GAME)
        return ServerConstructionReport(true, "REAL", command.installPath, steps)
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

    private fun completed(id: String, label: String, message: String) =
        ConstructionStep(id, label, ConstructionStepStatus.COMPLETED, message)

    data class Command(
        val serverName: String,
        val installPath: String,
        val steamCmdPath: String,
        val map: String,
        val gamePort: Int,
        val queryPort: Int,
        val rconPort: Int,
        val maxPlayers: Int,
        val serverPassword: String,
        val adminPassword: String,
        val gamePortAccess: CreateGamePortAccess.Command,
        val pveEnabled: Boolean = true,
        val xpMultiplier: Double = 1.0,
        val tamingSpeedMultiplier: Double = 1.0,
        val harvestAmountMultiplier: Double = 1.0,
        val eggHatchSpeedMultiplier: Double = 1.0,
        val babyMatureSpeedMultiplier: Double = 1.0,
    )

    companion object {
        const val GAME = "ASA"
        const val SERVER_ID = "asa-main"
        private val PROGRESS_STEPS = listOf(
            ConstructionProgressStepDefinition("steamcmd", "SteamCMDの準備・ダウンロード"),
            ConstructionProgressStepDefinition("install", "ARK: Survival Ascended Dedicated Serverのダウンロード"),
            ConstructionProgressStepDefinition("configuration", "GameUserSettings.iniとGame.iniの保存"),
            ConstructionProgressStepDefinition("firewall", "Windows Firewallの設定"),
            ConstructionProgressStepDefinition("start", "サーバー起動とポート待受確認"),
            ConstructionProgressStepDefinition("registration", "管理対象への登録"),
        )
    }
}
