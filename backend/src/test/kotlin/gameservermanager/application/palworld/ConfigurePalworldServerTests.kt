package gameservermanager.application.palworld

import gameservermanager.application.preflight.ManagedPathPolicy
import gameservermanager.infrastructure.palworld.FileSystemPalworldConfigurationWriter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ConfigurePalworldServerTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `入力値を設定しレポートにパスワードを含めない`() {
        val installPath = prepareDefaultSettings()
        val useCase = ConfigurePalworldServer(
            TemporaryManagedPathPolicy(tempDir),
            FileSystemPalworldConfigurationWriter(),
        )

        val report = useCase.execute(command(installPath))

        assertThat(report.completed).isTrue()
        assertThat(report.configuredKeys).contains("ServerPassword", "AdminPassword", "bIsUseBackupSaveData")
        assertThat(report.toString()).doesNotContain("join-secret", "admin-secret")
        assertThat(Path.of(report.settingsPath)).content()
            .contains(
                "ServerPassword=\"join-secret\"",
                "AdminPassword=\"admin-secret\"",
                "bIsUseBackupSaveData=True",
            )
    }

    @Test
    fun `引用符とバックスラッシュをINI用にエスケープする`() {
        val installPath = prepareDefaultSettings()
        val useCase = ConfigurePalworldServer(
            TemporaryManagedPathPolicy(tempDir),
            FileSystemPalworldConfigurationWriter(),
        )

        val report = useCase.execute(command(installPath).copy(serverName = "A\"B\\C"))

        assertThat(Path.of(report.settingsPath)).content()
            .contains("ServerName=\"A\\\"B\\\\C\"")
    }

    @Test
    fun `管理者パスワードが空なら設定ファイルを作らない`() {
        val installPath = prepareDefaultSettings()
        val useCase = ConfigurePalworldServer(
            TemporaryManagedPathPolicy(tempDir),
            FileSystemPalworldConfigurationWriter(),
        )

        assertThatThrownBy {
            useCase.execute(command(installPath).copy(adminPassword = ""))
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("管理者パスワードを入力してください")
        assertThat(installPath.resolve("Pal/Saved/Config/WindowsServer/PalWorldSettings.ini"))
            .doesNotExist()
    }

    private fun prepareDefaultSettings(): Path {
        val installPath = tempDir.resolve("servers/palworld/main/runtime")
        Files.createDirectories(installPath)
        Files.writeString(
            installPath.resolve("DefaultPalWorldSettings.ini"),
            """
                [/Script/Pal.PalGameWorldSettings]
                OptionSettings=(ServerName="Default",ServerPlayerMaxNum=32,ServerPassword="",AdminPassword="",PublicPort=8211,RCONEnabled=False,RCONPort=25575,RESTAPIEnabled=False,RESTAPIPort=8212,bIsUseBackupSaveData=False,ServerDescription="",ExpRate=1.000000,PalCaptureRate=1.000000,PalSpawnNumRate=1.000000,EnemyDropItemRate=1.000000,PalEggDefaultHatchingTime=2.000000,DeathPenalty=All,bIsPvP=False,bEnableFriendlyFire=False,BaseCampMaxNum=128,BaseCampWorkerMaxNum=15)
            """.trimIndent(),
        )
        return installPath
    }

    private fun command(installPath: Path): ConfigurePalworldServer.Command {
        return ConfigurePalworldServer.Command(
            serverName = "My Server",
            installPath = installPath.toString(),
            gamePort = 8211,
            rconPort = 25575,
            maxPlayers = 3,
            serverPassword = "join-secret",
            adminPassword = "admin-secret",
        )
    }

    private class TemporaryManagedPathPolicy(root: Path) : ManagedPathPolicy {
        private val normalizedRoot = root.toAbsolutePath().normalize()

        override fun isServerPathAllowed(path: String): Boolean {
            return Path.of(path).toAbsolutePath().normalize().startsWith(normalizedRoot.resolve("servers"))
        }

        override fun isToolPathAllowed(path: String): Boolean {
            return Path.of(path).toAbsolutePath().normalize().startsWith(normalizedRoot.resolve("tools"))
        }

        override fun managedRoot(): String {
            return normalizedRoot.toString()
        }
    }
}
