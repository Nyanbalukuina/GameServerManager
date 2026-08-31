package gameservermanager.palworld.application

import gameservermanager.palworld.application.PalworldServerProcessCommand
import gameservermanager.palworld.application.PalworldServerProcessManager
import gameservermanager.palworld.application.PalworldServerProcessSnapshot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import gameservermanager.palworld.domain.PalworldOperationHistoryEntry

class StopPalworldServerTests {
    @Test
    fun `ワールドを保存してから停止する`() {
        val processManager = FakeProcessManager()
        val client = RecordingManagementClient(processManager)
        val history = MemoryHistoryStore()
        val useCase = StopPalworldServer(client, processManager, history)

        val report = useCase.execute(StopPalworldServer.Command(8212, "admin-secret"))

        assertThat(report.completed).isTrue()
        assertThat(client.operations).containsExactly("save", "shutdown")
        assertThat(client.passwords).containsOnly("admin-secret")
        assertThat(report.toString()).doesNotContain("admin-secret")
        assertThat(history.entries.single().operation).isEqualTo("STOP")
    }

    private class RecordingManagementClient(
        private val processManager: FakeProcessManager,
    ) : PalworldManagementClient {
        val operations = mutableListOf<String>()
        val passwords = mutableListOf<String>()

        override fun save(port: Int, adminPassword: String) {
            operations += "save"
            passwords += adminPassword
        }

        override fun shutdown(
            port: Int,
            adminPassword: String,
            waitSeconds: Int,
            message: String,
        ) {
            operations += "shutdown"
            passwords += adminPassword
            processManager.alive = false
        }
    }

    private class FakeProcessManager : PalworldServerProcessManager {
        var alive = true

        override fun start(command: PalworldServerProcessCommand): PalworldServerProcessSnapshot {
            error("not used")
        }

        override fun current(): PalworldServerProcessSnapshot {
            return PalworldServerProcessSnapshot(1, alive, if (alive) null else 0, 8211, "server.log")
        }

        override fun stop() {
            alive = false
        }
    }

    private class MemoryHistoryStore : PalworldOperationHistoryStore {
        val entries = mutableListOf<PalworldOperationHistoryEntry>()

        override fun append(entry: PalworldOperationHistoryEntry) {
            entries += entry
        }

        override fun latest(limit: Int): List<PalworldOperationHistoryEntry> {
            return entries.takeLast(limit).reversed()
        }
    }
}
