package gameservermanager.application.construction

import gameservermanager.application.preflight.ManagedPathPolicy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RunDemoServerConstructionTests {
    @Test
    fun `最終入力からSteamCMDコマンドを生成し処理ステップを返す`() {
        val installer = RecordingSteamCmdInstaller()
        val useCase = RunDemoServerConstruction(AllowManagedPathPolicy(), installer)

        val report = useCase.execute(validCommand())

        assertThat(report.completed).isTrue()
        assertThat(report.mode).isEqualTo("DEMO")
        assertThat(report.steps).hasSize(4)
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
        )
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
