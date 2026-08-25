package gameservermanager.application.asa

import gameservermanager.application.construction.CreateGamePortAccess
import gameservermanager.application.server.GameServerRegistrationStore
import gameservermanager.domain.construction.ConstructionStep
import gameservermanager.domain.construction.ConstructionStepStatus
import gameservermanager.domain.construction.DemoConstructionReport
import gameservermanager.domain.server.GameServerRegistration
import gameservermanager.web.error.GameServerAlreadyExistsException
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock

// 実プログラムやFirewallに触れずASAの構成を一時領域へ再現する。
@Service
class RunDemoAsaServerConstruction(
    private val registrationStore: GameServerRegistrationStore,
    private val createGamePortAccess: CreateGamePortAccess,
) {
    fun execute(command: Command): DemoConstructionReport {
        if (registrationStore.findByGame("ASA") != null) throw GameServerAlreadyExistsException("ASA")
        val access = createGamePortAccess.execute(command.gamePortAccess)
        val workspace = Path.of(System.getProperty("java.io.tmpdir")).resolve("GameServerManagerDemo")
        val runtime = workspace.resolve("servers/asa/main/runtime")
        val settingsPath = runtime.resolve("ShooterGame/Saved/Config/WindowsServer/GameUserSettings.ini")
        val gameIniPath = runtime.resolve("ShooterGame/Saved/Config/WindowsServer/Game.ini")
        val executable = runtime.resolve("ShooterGame/Binaries/Win64/ArkAscendedServer.exe")
        Files.createDirectories(requireNotNull(settingsPath.parent))
        Files.createDirectories(requireNotNull(executable.parent))
        Files.createDirectories(workspace.resolve("logs"))
        Files.writeString(executable, "demo asa server", StandardCharsets.UTF_8)
        Files.writeString(settingsPath, settings(command), StandardCharsets.UTF_8)
        Files.writeString(gameIniPath, gameSettings(command), StandardCharsets.UTF_8)

        registrationStore.create(
            GameServerRegistration(
                "ASA", "asa-main", "DEMO", "STOPPED", command.serverName, command.installPath,
                workspace.toString(), command.gamePort, command.rconPort, Clock.systemUTC().instant(), access,
                command.gamePort + 1, command.queryPort, command.map, command.maxPlayers,
            ),
        )
        return DemoConstructionReport(
            true, "DEMO", workspace.toString(),
            listOf(
                completed("validate", "構築内容の検証", "ASAの入力内容を確認しました"),
                completed("install", "ASAデモ構成の配置", "一時領域へASA相当のフォルダーを作成しました"),
                completed("configuration", "ASA設定の保存", "GameUserSettings.iniとGame.iniを作成しました"),
                completed("registration", "管理対象への登録", "デモASAサーバーを登録しました"),
            ),
        )
    }

    private fun settings(command: Command) = AsaGameUserSettingsIni.update(
        "",
        linkedMapOf(
            "SessionSettings" to linkedMapOf("SessionName" to command.serverName),
            "ServerSettings" to linkedMapOf(
                "ServerPassword" to command.serverPassword,
                "ServerAdminPassword" to command.adminPassword,
                "RCONEnabled" to "True",
                "RCONPort" to command.rconPort.toString(),
                "ServerPVE" to command.pveEnabled.toString().replaceFirstChar(Char::uppercase),
                "XPMultiplier" to command.xpMultiplier.toString(),
                "TamingSpeedMultiplier" to command.tamingSpeedMultiplier.toString(),
                "HarvestAmountMultiplier" to command.harvestAmountMultiplier.toString(),
            ),
        ),
    )

    private fun gameSettings(command: Command) = AsaGameUserSettingsIni.update(
        "",
        linkedMapOf(
            "/Script/ShooterGame.ShooterGameMode" to linkedMapOf(
                "EggHatchSpeedMultiplier" to command.eggHatchSpeedMultiplier.toString(),
                "BabyMatureSpeedMultiplier" to command.babyMatureSpeedMultiplier.toString(),
            ),
        ),
    )

    private fun completed(id: String, label: String, message: String) =
        ConstructionStep(id, label, ConstructionStepStatus.COMPLETED, message)

    data class Command(
        val serverName: String,
        val installPath: String,
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
}
