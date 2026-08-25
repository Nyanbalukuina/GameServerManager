package gameservermanager.application.construction

import gameservermanager.application.preflight.ManagedPathPolicy
import gameservermanager.domain.construction.ConstructionStep
import gameservermanager.domain.construction.ConstructionStepStatus
import gameservermanager.domain.construction.DemoConstructionReport
import gameservermanager.application.server.GameServerRegistrationStore
import gameservermanager.domain.server.GameServerRegistration
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class RunDemoServerConstruction(
    private val managedPathPolicy: ManagedPathPolicy,
    private val steamCmdInstaller: SteamCmdInstaller,
    private val demoServerConfigurator: DemoServerConfigurator,
    private val registrationStore: GameServerRegistrationStore,
    private val createGamePortAccess: CreateGamePortAccess,
) {
    fun execute(command: Command): DemoConstructionReport {
        if (registrationStore.findByGame("PALWORLD") != null) {
            throw gameservermanager.web.error.GameServerAlreadyExistsException("PALWORLD")
        }
        require(managedPathPolicy.isServerPathAllowed(command.installPath)) {
            "インストール先が管理範囲外です"
        }
        require(managedPathPolicy.isToolPathAllowed(command.steamCmdPath)) {
            "SteamCMD保存先が管理範囲外です"
        }
        require(command.gamePort != command.rconPort) {
            "ゲームポートとRCONポートを分けてください"
        }
        require(command.adminPassword.isNotBlank()) {
            "管理者パスワードを入力してください"
        }

        val gamePortAccess = createGamePortAccess.execute(command.gamePortAccess)
        val installCommand = SteamCmdInstallCommand(
            steamCmdPath = command.steamCmdPath,
            installPath = command.installPath,
            appId = PALWORLD_SERVER_APP_ID,
        )
        val installResult = steamCmdInstaller.install(installCommand)
        demoServerConfigurator.configure(
            DemoServerConfigurationCommand(
                workspacePath = installResult.workspacePath,
                serverName = command.serverName,
                gamePort = command.gamePort,
                rconPort = command.rconPort,
                maxPlayers = command.maxPlayers,
                serverPassword = command.serverPassword,
                adminPassword = command.adminPassword,
                automationEnabled = command.automationEnabled,
                shutdownTime = command.shutdownTime,
                startupTime = command.startupTime,
                gamePortAccess = gamePortAccess,
                serverDescription = command.serverDescription,
                expRate = command.expRate, palCaptureRate = command.palCaptureRate,
                palSpawnRate = command.palSpawnRate, enemyDropRate = command.enemyDropRate,
                eggHatchingTime = command.eggHatchingTime, deathPenalty = command.deathPenalty,
                pvpEnabled = command.pvpEnabled, friendlyFireEnabled = command.friendlyFireEnabled,
                baseCampMaxNum = command.baseCampMaxNum, baseCampWorkerMaxNum = command.baseCampWorkerMaxNum,
            ),
        )
        registrationStore.create(
            GameServerRegistration(
                game = "PALWORLD",
                serverId = "palworld-main",
                mode = "DEMO",
                state = "STOPPED",
                serverName = command.serverName,
                installPath = command.installPath,
                workspacePath = installResult.workspacePath,
                gamePort = command.gamePort,
                rconPort = command.rconPort,
                createdAt = Clock.systemUTC().instant(),
                gamePortAccess = gamePortAccess,
            ),
        )

        val steps = listOf(
            completed("validate", "構築内容の検証", "管理ルートと入力内容を確認しました"),
            completed("steamcmd", "SteamCMDの準備", "デモSteamCMDを一時領域へ配置しました"),
            completed(
                "install",
                "Palworldサーバーの配置",
                "force_install_dir相当のデモ構成を作成しました",
            ),
            completed("data", "管理データの準備", "設定・ワールド・ログ用フォルダーを作成しました"),
            completed("configuration", "Palworld設定の保存", "パスワードを伏せたデモ設定を保存しました"),
            completed("automation", "自動運転設定の保存", "停止・起動設定をデモ保存しました"),
        )

        return DemoConstructionReport(
            completed = true,
            mode = "DEMO",
            workspacePath = installResult.workspacePath,
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
        private const val PALWORLD_SERVER_APP_ID = 2394010
    }
}
