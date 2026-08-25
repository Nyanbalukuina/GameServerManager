package gameservermanager.application.asa

import gameservermanager.application.construction.CreateGamePortAccess
import gameservermanager.application.construction.GameFirewallManager
import gameservermanager.application.construction.GameFirewallRuleCommand
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
) {
    @Synchronized
    fun execute(command: Command): ServerConstructionReport {
        if (registrationStore.findByGame(GAME) != null) throw GameServerAlreadyExistsException(GAME)
        require(command.rconPort !in setOf(command.gamePort, command.gamePort + 1, command.queryPort)) {
            "RCONポートはゲーム・Peer・Queryポートと分けてください"
        }

        val access = createGamePortAccess.execute(command.gamePortAccess)
        val steps = mutableListOf<ConstructionStep>()
        val steamCmdExecutable = Path.of(command.steamCmdPath).toAbsolutePath().normalize().resolve("steamcmd.exe")
        if (Files.isRegularFile(steamCmdExecutable)) {
            steps += completed("steamcmd", "SteamCMDの準備", "既存のSteamCMDを使用します")
        } else {
            prepareSteamCmd.execute(PrepareSteamCmd.Command(command.steamCmdPath))
            steps += completed("steamcmd", "SteamCMDの準備", "SteamCMDをダウンロードして配置しました")
        }

        installAsaServer.execute(InstallAsaServer.Command(command.steamCmdPath, command.installPath))
        steps += completed("install", "ASAサーバーのインストール", "SteamCMDによるインストールが完了しました")

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

        val firewallCommands = listOf(command.gamePort, command.gamePort + 1, command.queryPort).map { port ->
            GameFirewallRuleCommand(GAME, SERVER_ID, port, access.remoteAddresses())
        }
        val applied = mutableListOf<GameFirewallRuleCommand>()
        try {
            firewallCommands.forEach {
                firewallManager.apply(it)
                applied += it
            }
            steps += completed("firewall", "Windows Firewallの設定", "ASA用UDP受信規則を追加しました")

            startAsaServer.execute(
                StartAsaServer.Command(
                    command.installPath, command.map, command.gamePort, command.queryPort, command.maxPlayers,
                ),
            )
            steps += completed("start", "ASAサーバーの起動", "ゲームポートの待受を確認しました")

            registrationStore.create(
                GameServerRegistration(
                    GAME, SERVER_ID, "REAL", "RUNNING", command.serverName, command.installPath, "",
                    command.gamePort, command.rconPort, Clock.systemUTC().instant(), access,
                    command.gamePort + 1, command.queryPort, command.map, command.maxPlayers,
                ),
            )
            steps += completed("registration", "管理対象への登録", "ASAサーバーを登録しました")
        } catch (exception: RuntimeException) {
            applied.asReversed().forEach {
                try { firewallManager.remove(it) } catch (rollback: RuntimeException) { exception.addSuppressed(rollback) }
            }
            throw exception
        }

        return ServerConstructionReport(true, "REAL", command.installPath, steps)
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
    }
}
