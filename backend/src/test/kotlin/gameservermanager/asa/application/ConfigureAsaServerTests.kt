package gameservermanager.asa.application

import gameservermanager.shared.preflight.ManagedPathPolicy
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

// ASA設定値の検証と保存命令を確認する。
class ConfigureAsaServerTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `ASA設定を保存してレポートにパスワードを含めない`() {
        val writer = RecordingConfigurationWriter()
        val configureAsaServer = ConfigureAsaServer(
            TemporaryManagedPathPolicy(tempDir),
            writer,
        )

        // ASAの初期設定を保存する。
        val report = configureAsaServer.execute(command())

        // 正しい保存先と設定内容がWriterへ渡されたことを確認する。
        assertThat(report.completed).isTrue()
        assertThat(report.settingsPath).endsWith("GameUserSettings.ini")
        assertThat(report.configuredKeys).contains(
            "SessionName",
            "ServerPassword",
            "ServerAdminPassword",
            "RCONEnabled",
            "RCONPort",
        )
        assertThat(report.toString()).doesNotContain("join-secret", "admin-secret")
        val userSettings = writer.commands.first { it.settingsPath.fileName.toString() == "GameUserSettings.ini" }
        val gameSettings = writer.commands.first { it.settingsPath.fileName.toString() == "Game.ini" }
        assertThat(userSettings.sections["SessionSettings"])
            .containsEntry("SessionName", "GSM ASA Server")
        assertThat(userSettings.sections["ServerSettings"])
            .containsEntry("RCONPort", "27020")
        assertThat(gameSettings.sections["/Script/ShooterGame.ShooterGameMode"])
            .containsEntry("EggHatchSpeedMultiplier", "1.0")
            .containsEntry("BabyMatureSpeedMultiplier", "1.0")
            .containsEntry("bUseSingleplayerSettings", "True")
    }

    @Test
    fun `管理者パスワードが空なら設定を保存しない`() {
        val writer = RecordingConfigurationWriter()
        val configureAsaServer = ConfigureAsaServer(
            TemporaryManagedPathPolicy(tempDir),
            writer,
        )

        // 管理者パスワードを空にして入力検証を実行する。
        assertThatThrownBy {
            configureAsaServer.execute(command().copy(adminPassword = ""))
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("管理者パスワードを入力してください")

        assertThat(writer.called).isFalse()
    }

    // 正常なASA設定コマンドを作る。
    private fun command(): ConfigureAsaServer.Command {
        return ConfigureAsaServer.Command(
            serverName = "GSM ASA Server",
            installPath = tempDir.resolve("servers/asa/main/runtime").toString(),
            rconPort = 27020,
            serverPassword = "join-secret",
            adminPassword = "admin-secret",
            useSingleplayerSettings = true,
        )
    }

    // Writerへ渡された設定を記録する。
    private class RecordingConfigurationWriter : AsaConfigurationWriter {
        var called = false
        val commands = mutableListOf<AsaConfigurationWriteCommand>()

        override fun write(command: AsaConfigurationWriteCommand): AsaConfigurationWriteResult {
            called = true
            commands += command

            return AsaConfigurationWriteResult(
                settingsPath = command.settingsPath.toString(),
                backupPath = null,
            )
        }
    }

    // テスト用ルート内だけを管理対象として許可する。
    private class TemporaryManagedPathPolicy(root: Path) : ManagedPathPolicy {
        private val normalizedRoot = root.toAbsolutePath().normalize()

        override fun isServerPathAllowed(path: String): Boolean {
            return Path.of(path)
                .toAbsolutePath()
                .normalize()
                .startsWith(normalizedRoot.resolve("servers"))
        }

        override fun isToolPathAllowed(path: String): Boolean {
            return Path.of(path)
                .toAbsolutePath()
                .normalize()
                .startsWith(normalizedRoot.resolve("tools"))
        }

        override fun managedRoot(): String {
            return normalizedRoot.toString()
        }
    }
}
