package gameservermanager.infrastructure.palworld

import gameservermanager.application.palworld.PalworldConfigurationWriteCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class FileSystemPalworldConfigurationWriterTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `初回は既定設定からPalworld設定を生成する`() {
        val defaultPath = writeDefaultSettings()
        val settingsPath = settingsPath()
        val writer = FileSystemPalworldConfigurationWriter()

        val result = writer.write(command(defaultPath, settingsPath))

        val content = Files.readString(settingsPath)
        assertThat(result.backupPath).isNull()
        assertThat(content).contains(
            "ServerName=\"My Server\"",
            "ServerPlayerMaxNum=3",
            "ServerPassword=\"join-secret\"",
            "AdminPassword=\"admin-secret\"",
            "PublicPort=8211",
            "RCONEnabled=True",
            "RCONPort=25575",
            "RESTAPIEnabled=True",
            "RESTAPIPort=8212",
            "Difficulty=None",
        )
    }

    @Test
    fun `既存設定を管理ルートのbackupsへ退避してから更新する`() {
        val defaultPath = writeDefaultSettings()
        val settingsPath = settingsPath()
        Files.createDirectories(requireNotNull(settingsPath.parent))
        Files.writeString(settingsPath, defaultContent().replace("ServerName=\"Default\"", "ServerName=\"Old\""))
        val clock = Clock.fixed(Instant.parse("2026-08-10T03:00:00Z"), ZoneOffset.UTC)
        val writer = FileSystemPalworldConfigurationWriter(clock)

        val result = writer.write(command(defaultPath, settingsPath))

        val backupPath = Path.of(requireNotNull(result.backupPath))
        assertThat(backupPath).hasFileName("PalWorldSettings-20260810-030000-000.ini")
        assertThat(backupPath).content().contains("ServerName=\"Old\"")
        assertThat(settingsPath).content().contains("ServerName=\"My Server\"")
    }

    @Test
    fun `エスケープ済みの引用符とバックスラッシュを保持する`() {
        val defaultPath = writeDefaultSettings()
        val settingsPath = settingsPath()
        val writer = FileSystemPalworldConfigurationWriter()
        val command = command(defaultPath, settingsPath).copy(
            values = linkedMapOf("ServerName" to "\"A\\\"B\\\\C\""),
        )

        writer.write(command)

        assertThat(settingsPath).content().contains("ServerName=\"A\\\"B\\\\C\"")
    }

    private fun command(defaultPath: Path, settingsPath: Path): PalworldConfigurationWriteCommand {
        return PalworldConfigurationWriteCommand(
            defaultSettingsPath = defaultPath,
            settingsPath = settingsPath,
            backupDirectory = tempDir.resolve("backups/palworld-main/config"),
            values = linkedMapOf(
                "ServerName" to "\"My Server\"",
                "ServerPlayerMaxNum" to "3",
                "ServerPassword" to "\"join-secret\"",
                "AdminPassword" to "\"admin-secret\"",
                "PublicPort" to "8211",
                "RCONEnabled" to "True",
                "RCONPort" to "25575",
                "RESTAPIEnabled" to "True",
                "RESTAPIPort" to "8212",
                "bIsUseBackupSaveData" to "True",
            ),
        )
    }

    private fun writeDefaultSettings(): Path {
        val path = tempDir.resolve("servers/palworld/main/runtime/DefaultPalWorldSettings.ini")
        Files.createDirectories(requireNotNull(path.parent))
        Files.writeString(path, defaultContent())
        return path
    }

    private fun settingsPath(): Path {
        return tempDir.resolve(
            "servers/palworld/main/runtime/Pal/Saved/Config/WindowsServer/PalWorldSettings.ini",
        )
    }

    private fun defaultContent(): String {
        return """
            [/Script/Pal.PalGameWorldSettings]
            OptionSettings=(Difficulty=None,ServerName="Default",ServerPlayerMaxNum=32,ServerPassword="",AdminPassword="",PublicPort=8211,RCONEnabled=False,RCONPort=25575,RESTAPIEnabled=False,RESTAPIPort=8212,bIsUseBackupSaveData=False)
        """.trimIndent()
    }
}
