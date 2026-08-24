package gameservermanager.application.palworld

import gameservermanager.application.preflight.ManagedPathPolicy
import gameservermanager.domain.palworld.PalworldConfigurationReport
import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
class ConfigurePalworldServer(
    private val managedPathPolicy: ManagedPathPolicy,
    private val configurationWriter: PalworldConfigurationWriter,
) {
    fun execute(command: Command): PalworldConfigurationReport {
        require(managedPathPolicy.isServerPathAllowed(command.installPath)) {
            "インストール先が管理範囲外です"
        }
        require(command.gamePort in 1..65535) {
            "ゲームポートは1から65535の範囲で指定してください"
        }
        require(command.rconPort in 1..65535) {
            "RCONポートは1から65535の範囲で指定してください"
        }
        require(command.gamePort != command.rconPort) {
            "ゲームポートとRCONポートを分けてください"
        }
        require(command.maxPlayers in 1..32) {
            "最大プレイヤー数は1から32の範囲で指定してください"
        }
        require(command.adminPassword.isNotBlank()) {
            "管理者パスワードを入力してください"
        }

        val installPath = Path.of(command.installPath).toAbsolutePath().normalize()
        val values = linkedMapOf(
            "ServerName" to quoted(command.serverName),
            "ServerPlayerMaxNum" to command.maxPlayers.toString(),
            "ServerPassword" to quoted(command.serverPassword),
            "AdminPassword" to quoted(command.adminPassword),
            "PublicPort" to command.gamePort.toString(),
            "RCONEnabled" to "True",
            "RCONPort" to command.rconPort.toString(),
            "RESTAPIEnabled" to "True",
            "RESTAPIPort" to REST_API_PORT.toString(),
            "bIsUseBackupSaveData" to "True",
        )
        val result = configurationWriter.write(
            PalworldConfigurationWriteCommand(
                defaultSettingsPath = installPath.resolve("DefaultPalWorldSettings.ini"),
                settingsPath = installPath.resolve(
                    "Pal/Saved/Config/WindowsServer/PalWorldSettings.ini",
                ),
                backupDirectory = Path.of(managedPathPolicy.managedRoot())
                    .resolve("backups/palworld-main/config"),
                values = values,
            ),
        )

        return PalworldConfigurationReport(
            completed = true,
            settingsPath = result.settingsPath,
            backupPath = result.backupPath,
            configuredKeys = values.keys.toList(),
        )
    }

    private fun quoted(value: String): String {
        require(!value.contains('\n') && !value.contains('\r')) {
            "設定値に改行は使用できません"
        }
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        return "\"$escaped\""
    }

    data class Command(
        val serverName: String,
        val installPath: String,
        val gamePort: Int,
        val rconPort: Int,
        val maxPlayers: Int,
        val serverPassword: String,
        val adminPassword: String,
    )

    companion object {
        const val REST_API_PORT = 8212
    }
}
