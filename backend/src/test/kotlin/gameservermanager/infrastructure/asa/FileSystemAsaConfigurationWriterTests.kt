package gameservermanager.infrastructure.asa

import gameservermanager.application.asa.AsaConfigurationWriteCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

// ASA設定ファイルの作成・更新・バックアップを確認する。
class FileSystemAsaConfigurationWriterTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `GameUserSettings iniがなければ新しく作成する`() {
        val settingsPath = settingsPath()
        val writer = FileSystemAsaConfigurationWriter()

        // 空の保存先へASA設定を書き込む。
        val result = writer.write(command(settingsPath))

        // 設定ファイルが作成され、初回はバックアップがないことを確認する。
        assertThat(result.backupPath).isNull()
        assertThat(settingsPath).isRegularFile()
        assertThat(settingsPath).content().contains(
            "[SessionSettings]",
            "SessionName=GSM ASA Server",
            "[ServerSettings]",
            "ServerPassword=join-secret",
            "ServerAdminPassword=admin-secret",
            "RCONEnabled=True",
            "RCONPort=27020",
        )
    }

    @Test
    fun `既存設定をバックアップしてから対象項目を更新する`() {
        val settingsPath = settingsPath()
        Files.createDirectories(requireNotNull(settingsPath.parent))
        Files.writeString(
            settingsPath,
            """
                [SessionSettings]
                SessionName=Old Server

                [ServerSettings]
                ServerPassword=old-password
                ServerCrosshair=True
            """.trimIndent(),
        )

        // バックアップ名を固定して確認できるようにする。
        val clock = Clock.fixed(Instant.parse("2026-08-24T03:00:00Z"), ZoneOffset.UTC)
        val writer = FileSystemAsaConfigurationWriter(clock)
        val result = writer.write(command(settingsPath))

        // 既存設定のバックアップと新しい設定を確認する。
        val backupPath = Path.of(requireNotNull(result.backupPath))
        assertThat(backupPath).hasFileName("GameUserSettings-20260824-030000-000.ini")
        assertThat(backupPath).content().contains(
            "SessionName=Old Server",
            "ServerPassword=old-password",
        )
        assertThat(settingsPath).content().contains(
            "SessionName=GSM ASA Server",
            "ServerPassword=join-secret",
            "ServerCrosshair=True",
        )
    }

    // テスト用のGameUserSettings.ini保存先を返す。
    private fun settingsPath(): Path {
        return tempDir.resolve(
            "servers/asa/main/runtime/ShooterGame/Saved/Config/WindowsServer/GameUserSettings.ini",
        )
    }

    // テストで保存するASA設定を作る。
    private fun command(settingsPath: Path): AsaConfigurationWriteCommand {
        return AsaConfigurationWriteCommand(
            settingsPath = settingsPath,
            backupDirectory = tempDir.resolve("backups/asa-main/config"),
            sections = linkedMapOf(
                "SessionSettings" to linkedMapOf(
                    "SessionName" to "GSM ASA Server",
                ),
                "ServerSettings" to linkedMapOf(
                    "ServerPassword" to "join-secret",
                    "ServerAdminPassword" to "admin-secret",
                    "RCONEnabled" to "True",
                    "RCONPort" to "27020",
                ),
            ),
        )
    }
}