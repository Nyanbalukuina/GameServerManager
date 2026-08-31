package gameservermanager.palworld.application

import gameservermanager.palworld.domain.PalworldServerState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GetPalworldServerStatusTests {
    @Test
    fun `未起動状態を返す`() {
        val status = GetPalworldServerStatus(FixedProcessManager(null)).execute()

        assertThat(status.state).isEqualTo(PalworldServerState.NOT_STARTED)
        assertThat(status.processId).isNull()
    }

    @Test
    fun `終了したプロセスの終了コードを返す`() {
        val snapshot = PalworldServerProcessSnapshot(99, false, 0, 8211, "server.log")

        val status = GetPalworldServerStatus(FixedProcessManager(snapshot)).execute()

        assertThat(status.state).isEqualTo(PalworldServerState.STOPPED)
        assertThat(status.message).contains("終了コード: 0")
    }

    private class FixedProcessManager(
        private val snapshot: PalworldServerProcessSnapshot?,
    ) : PalworldServerProcessManager {
        override fun start(command: PalworldServerProcessCommand): PalworldServerProcessSnapshot {
            error("not used")
        }

        override fun current(): PalworldServerProcessSnapshot? {
            return snapshot
        }

        override fun stop() {
        }
    }
}
