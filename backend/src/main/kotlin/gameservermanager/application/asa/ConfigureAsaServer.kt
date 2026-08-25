package gameservermanager.application.asa

import gameservermanager.application.preflight.ManagedPathPolicy
import gameservermanager.domain.asa.AsaConfigurationReport
import org.springframework.stereotype.Service
import java.nio.file.Path

// ASAの初期設定をGameUserSettings.iniへ保存する。
@Service
class ConfigureAsaServer(
    private val managedPathPolicy: ManagedPathPolicy,
    private val configurationWriter: AsaConfigurationWriter,
) {
    // 入力値を検証し、ASA設定ファイルを作成または更新する。
    fun execute(command: Command): AsaConfigurationReport {
        require(managedPathPolicy.isServerPathAllowed(command.installPath)) {
            "ASAインストール先が管理範囲外です"
        }
        require(command.serverName.isNotBlank()) { "サーバー名を入力してください" }
        require(command.adminPassword.isNotBlank()) { "管理者パスワードを入力してください" }
        require(command.rconPort in 1..65535) { "RCONポートは1から65535の範囲で指定してください" }
        require(command.xpMultiplier in 0.1..100.0) { "経験値倍率は0.1から100の範囲で指定してください" }
        require(command.tamingSpeedMultiplier in 0.1..100.0) { "テイム速度は0.1から100の範囲で指定してください" }
        require(command.harvestAmountMultiplier in 0.1..100.0) { "採取量倍率は0.1から100の範囲で指定してください" }
        require(command.eggHatchSpeedMultiplier in 0.1..100.0) { "孵化速度は0.1から100の範囲で指定してください" }
        require(command.babyMatureSpeedMultiplier in 0.1..100.0) { "成熟速度は0.1から100の範囲で指定してください" }

        // INIへ別の設定を挿入できないように改行を拒否する。
        requireSingleLine(command.serverName, "サーバー名")
        requireSingleLine(command.serverPassword, "サーバーパスワード")
        requireSingleLine(command.adminPassword, "管理者パスワード")

        val installPath = Path.of(command.installPath).toAbsolutePath().normalize()

        // GameUserSettings.iniへ保存するセクションと設定を作る。
        val sections = linkedMapOf(
            "SessionSettings" to linkedMapOf(
                "SessionName" to command.serverName,
            ),
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
        )

        // ASA設定をファイルシステムへ保存する。
        val result = configurationWriter.write(
            AsaConfigurationWriteCommand(
                settingsPath = installPath.resolve(
                    "ShooterGame/Saved/Config/WindowsServer/GameUserSettings.ini",
                ),
                backupDirectory = Path.of(managedPathPolicy.managedRoot())
                    .resolve("backups/asa-main/config"),
                sections = sections,
            ),
        )
        val gameSections = linkedMapOf(
            "/Script/ShooterGame.ShooterGameMode" to linkedMapOf(
                "EggHatchSpeedMultiplier" to command.eggHatchSpeedMultiplier.toString(),
                "BabyMatureSpeedMultiplier" to command.babyMatureSpeedMultiplier.toString(),
            ),
        )
        configurationWriter.write(
            AsaConfigurationWriteCommand(
                settingsPath = installPath.resolve("ShooterGame/Saved/Config/WindowsServer/Game.ini"),
                backupDirectory = Path.of(managedPathPolicy.managedRoot()).resolve("backups/asa-main/config"),
                sections = gameSections,
            ),
        )

        // パスワード本体を含めず、設定結果だけを返す。
        return AsaConfigurationReport(
            completed = true,
            settingsPath = result.settingsPath,
            backupPath = result.backupPath,
            configuredKeys = sections.values.flatMap { it.keys } + gameSections.values.flatMap { it.keys },
        )
    }

    // 設定値に改行が含まれていないことを確認する。
    private fun requireSingleLine(value: String, label: String) {
        require(!value.contains('\n') && !value.contains('\r')) {
            "$label に改行は使用できません"
        }
    }

    // ASA初期設定に必要な入力値を保持する。
    data class Command(
        val serverName: String,
        val installPath: String,
        val rconPort: Int,
        val serverPassword: String,
        val adminPassword: String,
        val pveEnabled: Boolean = true,
        val xpMultiplier: Double = 1.0,
        val tamingSpeedMultiplier: Double = 1.0,
        val harvestAmountMultiplier: Double = 1.0,
        val eggHatchSpeedMultiplier: Double = 1.0,
        val babyMatureSpeedMultiplier: Double = 1.0,
    )
}
