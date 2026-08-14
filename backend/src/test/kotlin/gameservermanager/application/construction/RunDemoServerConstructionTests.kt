package gameservermanager.application.construction

import gameservermanager.application.preflight.ManagedPathPolicy
import gameservermanager.application.server.GameServerRegistrationStore
import gameservermanager.domain.server.GameServerRegistration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RunDemoServerConstructionTests {
    @Test
    fun `最終入力からSteamCMDコマンドを生成し処理ステップを返す`() {
        val installer = RecordingSteamCmdInstaller()
        val configurator = RecordingDemoServerConfigurator()
        val registrations = MemoryRegistrationStore()
        val useCase = RunDemoServerConstruction(
            AllowManagedPathPolicy(),
            installer,
            configurator,
            registrations,
            CreateGamePortAccess(),
        )

        val report = useCase.execute(validCommand())

        assertThat(report.completed).isTrue()
        assertThat(report.mode).isEqualTo("DEMO")
        assertThat(report.steps).hasSize(6)
        assertThat(installer.command?.arguments()).containsExactly(
            "+force_install_dir",
            "C:\\GameServerManager\\servers\\palworld\\main\\runtime",
            "+login",
            "anonymous",
            "+app_update",
            "2394010",
            "validate",
            "+quit",
        )
        assertThat(report.toString()).doesNotContain("admin-password", "server-password")
        assertThat(configurator.command?.automationEnabled).isTrue()
        assertThat(configurator.command?.shutdownTime).isEqualTo("04:00")
        assertThat(registrations.findByGame("PALWORLD")?.mode).isEqualTo("DEMO")
        assertThat(registrations.findByGame("PALWORLD")?.gamePortAccess?.tailscale).isTrue()
    }

    private fun validCommand(): RunDemoServerConstruction.Command {
        return RunDemoServerConstruction.Command(
            serverName = "Palworld Server",
            installPath = "C:\\GameServerManager\\servers\\palworld\\main\\runtime",
            steamCmdPath = "C:\\GameServerManager\\tools\\steamcmd",
            gamePort = 8211,
            rconPort = 25575,
            maxPlayers = 3,
            serverPassword = "server-password",
            adminPassword = "admin-password",
            automationEnabled = true,
            shutdownTime = "04:00",
            startupTime = "09:00",
            backupAfterShutdown = true,
            gamePortAccess = CreateGamePortAccess.Command(
                localSubnet = true,
                tailscale = true,
                customRemoteAddresses = "",
                allowAny = false,
            ),
        )
    }

    private class RecordingDemoServerConfigurator : DemoServerConfigurator {
        var command: DemoServerConfigurationCommand? = null

        override fun configure(command: DemoServerConfigurationCommand) {
            this.command = command
        }
    }

    private class MemoryRegistrationStore : GameServerRegistrationStore {
        private val registrations = mutableListOf<GameServerRegistration>()
        override fun findAll(): List<GameServerRegistration> = registrations.toList()
        override fun findByGame(game: String): GameServerRegistration? = registrations.singleOrNull { it.game == game }
        override fun create(registration: GameServerRegistration) {
            registrations += registration
        }
        override fun update(registration: GameServerRegistration) = Unit
        override fun delete(game: String) = Unit
    }

    private class RecordingSteamCmdInstaller : SteamCmdInstaller {
        var command: SteamCmdInstallCommand? = null

        override fun install(command: SteamCmdInstallCommand): SteamCmdInstallResult {
            this.command = command
            return SteamCmdInstallResult("demo", "demo/steamcmd.exe", "demo/PalServer.exe")
        }
    }

    private class AllowManagedPathPolicy : ManagedPathPolicy {
        override fun isServerPathAllowed(path: String): Boolean {
            return true
        }

        override fun isToolPathAllowed(path: String): Boolean {
            return true
        }

        override fun managedRoot(): String {
            return "C:\\GameServerManager"
        }
    }
}
