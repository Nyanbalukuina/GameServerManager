package gameservermanager.application.construction

import gameservermanager.application.preflight.ManagedPathPolicy
import gameservermanager.domain.construction.ConstructionStep
import gameservermanager.domain.construction.ConstructionStepStatus
import gameservermanager.domain.construction.DemoConstructionReport
import org.springframework.stereotype.Service

@Service
class RunDemoServerConstruction(
    private val managedPathPolicy: ManagedPathPolicy,
    private val steamCmdInstaller: SteamCmdInstaller,
) {
    fun execute(command: Command): DemoConstructionReport {
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

        val installCommand = SteamCmdInstallCommand(
            steamCmdPath = command.steamCmdPath,
            installPath = command.installPath,
            appId = PALWORLD_SERVER_APP_ID,
        )
        val installResult = steamCmdInstaller.install(installCommand)

        val steps = listOf(
            completed("validate", "構築内容の検証", "管理ルートと入力内容を確認しました"),
            completed("steamcmd", "SteamCMDの準備", "デモSteamCMDを一時領域へ配置しました"),
            completed(
                "install",
                "Palworldサーバーの配置",
                "force_install_dir相当のデモ構成を作成しました",
            ),
            completed("data", "管理データの準備", "設定・ワールド・ログ用フォルダーを作成しました"),
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
    )

    companion object {
        private const val PALWORLD_SERVER_APP_ID = 2394010
    }
}
