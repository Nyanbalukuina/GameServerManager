package gameservermanager.asa.application

import gameservermanager.shared.server.GameServerProcessCommand
import gameservermanager.shared.server.GameServerProcessManager
import gameservermanager.shared.server.GameServerProcessSnapshot
import gameservermanager.asa.domain.AsaServerState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StopAsaServerTests {
    @Test
    fun `SaveWorldとDoExitを順番に送って停止する`() {
        val processManager = TestProcessManager()
        val client = TestManagementClient { command ->
            if (command == "DoExit") processManager.alive = false
        }
        val status = StopAsaServer(client, processManager)
            .execute(StopAsaServer.Command(27020, "admin-secret"))

        assertThat(client.commands).containsExactly("SaveWorld", "DoExit")
        assertThat(status.state).isEqualTo(AsaServerState.STOPPED)
    }

    private class TestManagementClient(private val onCommand: (String) -> Unit) : AsaManagementClient {
        val commands = mutableListOf<String>()
        override fun execute(port: Int, password: String, command: String): String {
            commands += command
            onCommand(command)
            return ""
        }
    }

    private class TestProcessManager : GameServerProcessManager {
        var alive = true
        override fun start(command: GameServerProcessCommand) = snapshot()
        override fun current(serverId: String) = snapshot()
        override fun stop(serverId: String) { alive = false }
        private fun snapshot() = GameServerProcessSnapshot("asa-main", 123L, alive, if (alive) null else 0, "asa.log")
    }
}
