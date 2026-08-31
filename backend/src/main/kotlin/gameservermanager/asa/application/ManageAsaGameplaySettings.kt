package gameservermanager.asa.application

import gameservermanager.shared.server.GameServerRegistrationStore
import gameservermanager.shared.preflight.ManagedPathPolicy
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

@Service
class ManageAsaGameplaySettings(
    private val registrationStore: GameServerRegistrationStore,
    private val configurationWriter: AsaConfigurationWriter,
    private val managedPathPolicy: ManagedPathPolicy,
) {
    fun get(): Settings {
        val server = registration()
        val content = readFile(settingsPath(server.installPath, server.workspacePath, server.mode))
        val gameContent = readFile(gameIniPath(server.installPath, server.workspacePath, server.mode))
        return Settings(
            serverName = AsaGameUserSettingsIni.read(content, "SessionSettings", "SessionName") ?: server.serverName,
            maxPlayers = server.maxPlayers ?: 70,
            pveEnabled = readBoolean(content, "ServerPVE", true),
            xpMultiplier = readDouble(content, "XPMultiplier", 1.0),
            tamingSpeedMultiplier = readDouble(content, "TamingSpeedMultiplier", 1.0),
            harvestAmountMultiplier = readDouble(content, "HarvestAmountMultiplier", 1.0),
            eggHatchSpeedMultiplier = readGameDouble(gameContent, "EggHatchSpeedMultiplier", 1.0),
            babyMatureSpeedMultiplier = readGameDouble(gameContent, "BabyMatureSpeedMultiplier", 1.0),
            useSingleplayerSettings = readGameBoolean(gameContent, "bUseSingleplayerSettings", false),
        )
    }

    fun update(settings: Settings): Settings {
        val server = registration()
        require(server.state != "RUNNING") { "設定を変更する前にARKサーバーを停止してください" }
        require(settings.serverName.isNotBlank()) { "サーバー名を入力してください" }
        require(settings.serverName.length <= 100 && !settings.serverName.contains('\n') && !settings.serverName.contains('\r')) {
            "サーバー名は100文字以内の1行で入力してください"
        }
        require(settings.maxPlayers in 1..70) { "最大プレイヤー数は1から70の範囲で指定してください" }
        validateMultiplier(settings.xpMultiplier, "経験値倍率")
        validateMultiplier(settings.tamingSpeedMultiplier, "テイム速度")
        validateMultiplier(settings.harvestAmountMultiplier, "採取量倍率")
        validateMultiplier(settings.eggHatchSpeedMultiplier, "孵化速度")
        validateMultiplier(settings.babyMatureSpeedMultiplier, "成熟速度")

        val path = settingsPath(server.installPath, server.workspacePath, server.mode)
        configurationWriter.write(
            AsaConfigurationWriteCommand(
                path,
                Path.of(managedPathPolicy.managedRoot()).resolve("backups/asa-main/config"),
                linkedMapOf(
                    "SessionSettings" to linkedMapOf("SessionName" to settings.serverName),
                    "ServerSettings" to linkedMapOf(
                        "ServerPVE" to settings.pveEnabled.toString().replaceFirstChar(Char::uppercase),
                        "XPMultiplier" to settings.xpMultiplier.toString(),
                        "TamingSpeedMultiplier" to settings.tamingSpeedMultiplier.toString(),
                        "HarvestAmountMultiplier" to settings.harvestAmountMultiplier.toString(),
                    ),
                ),
            ),
        )
        configurationWriter.write(
            AsaConfigurationWriteCommand(
                gameIniPath(server.installPath, server.workspacePath, server.mode),
                Path.of(managedPathPolicy.managedRoot()).resolve("backups/asa-main/config"),
                linkedMapOf(
                    "/Script/ShooterGame.ShooterGameMode" to linkedMapOf(
                        "EggHatchSpeedMultiplier" to settings.eggHatchSpeedMultiplier.toString(),
                        "BabyMatureSpeedMultiplier" to settings.babyMatureSpeedMultiplier.toString(),
                        "bUseSingleplayerSettings" to settings.useSingleplayerSettings.toString().replaceFirstChar(Char::uppercase),
                    ),
                ),
            ),
        )
        registrationStore.update(server.copy(serverName = settings.serverName, maxPlayers = settings.maxPlayers))
        return settings
    }

    private fun registration() = requireNotNull(registrationStore.findByGame("ASA")) {
        "ASAサーバーは登録されていません"
    }

    private fun settingsPath(installPath: String, workspacePath: String, mode: String): Path =
        serverRoot(installPath, workspacePath, mode)
            .resolve("ShooterGame/Saved/Config/WindowsServer/GameUserSettings.ini")

    private fun gameIniPath(installPath: String, workspacePath: String, mode: String): Path =
        serverRoot(installPath, workspacePath, mode)
            .resolve("ShooterGame/Saved/Config/WindowsServer/Game.ini")

    private fun serverRoot(installPath: String, workspacePath: String, mode: String): Path =
        if (mode == "DEMO") Path.of(workspacePath).resolve("servers/asa/main/runtime") else Path.of(installPath)

    private fun readFile(path: Path) =
        if (Files.isRegularFile(path)) Files.readString(path, StandardCharsets.UTF_8) else ""

    private fun readBoolean(content: String, key: String, default: Boolean): Boolean {
        val value = AsaGameUserSettingsIni.read(content, "ServerSettings", key) ?: return default
        return when {
            value.equals("true", ignoreCase = true) -> true
            value.equals("false", ignoreCase = true) -> false
            else -> default
        }
    }

    private fun readDouble(content: String, key: String, default: Double) =
        AsaGameUserSettingsIni.read(content, "ServerSettings", key)?.toDoubleOrNull() ?: default

    private fun readGameDouble(content: String, key: String, default: Double) =
        AsaGameUserSettingsIni.read(content, "/Script/ShooterGame.ShooterGameMode", key)?.toDoubleOrNull() ?: default

    private fun readGameBoolean(content: String, key: String, default: Boolean): Boolean {
        val value = AsaGameUserSettingsIni.read(content, "/Script/ShooterGame.ShooterGameMode", key) ?: return default
        return when {
            value.equals("true", ignoreCase = true) -> true
            value.equals("false", ignoreCase = true) -> false
            else -> default
        }
    }

    private fun validateMultiplier(value: Double, label: String) {
        require(value.isFinite() && value in 0.1..100.0) { "$label は0.1から100の範囲で指定してください" }
    }

    data class Settings(
        val serverName: String,
        val maxPlayers: Int,
        val pveEnabled: Boolean,
        val xpMultiplier: Double,
        val tamingSpeedMultiplier: Double,
        val harvestAmountMultiplier: Double,
        val eggHatchSpeedMultiplier: Double,
        val babyMatureSpeedMultiplier: Double,
        val useSingleplayerSettings: Boolean,
    )
}
