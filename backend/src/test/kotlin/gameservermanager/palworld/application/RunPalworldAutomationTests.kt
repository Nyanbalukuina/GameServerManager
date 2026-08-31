package gameservermanager.palworld.application

import gameservermanager.shared.preflight.ManagedPathPolicy
import gameservermanager.shared.preflight.ServerEnvironmentInspector
import gameservermanager.palworld.domain.PalworldAutomationRuntimeState
import gameservermanager.palworld.domain.PalworldAutomationSettings
import gameservermanager.palworld.domain.PalworldOperationHistoryEntry
import gameservermanager.palworld.domain.PalworldOperationReport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.LocalDateTime

class RunPalworldAutomationTests {
    @Test
    fun `停止時間になるとサーバーを停止する`() {
        val dependencies = dependencies(running = true)
        given(dependencies.stop.execute(StopPalworldServer.Command(8212, "admin-password")))
            .willReturn(PalworldOperationReport(true, "STOP", "stopped"))

        val result = dependencies.runner.execute(LocalDateTime.parse("2026-08-10T04:00:00"))

        verify(dependencies.stop).execute(StopPalworldServer.Command(8212, "admin-password"))
        assertThat(result.runtime.stoppedBySchedule).isTrue()
    }

    private fun dependencies(running: Boolean): Dependencies {
        val configure = mock(ConfigurePalworldAutomation::class.java)
        val store = MemoryStore()
        val managedPathPolicy = mock(ManagedPathPolicy::class.java)
        val environmentInspector = mock(ServerEnvironmentInspector::class.java)
        val processManager = mock(PalworldServerProcessManager::class.java)
        val passwordProvider = FixedPasswordProvider
        val stop = mock(StopPalworldServer::class.java)
        val start = mock(StartPalworldServer::class.java)
        val history = EmptyHistoryStore
        given(configure.get()).willReturn(SETTINGS)
        given(managedPathPolicy.managedRoot()).willReturn("C:\\GameServerManager")
        if (running) {
            given(processManager.current()).willReturn(
                PalworldServerProcessSnapshot(1, true, null, 8211, "log"),
                null,
            )
        } else {
            given(processManager.current()).willReturn(null)
        }
        given(environmentInspector.isUdpPortAvailable(8211)).willReturn(true)
        val runner = RunPalworldAutomation(
            configure,
            store,
            managedPathPolicy,
            environmentInspector,
            processManager,
            passwordProvider,
            stop,
            start,
            history,
        )
        return Dependencies(runner, stop)
    }

    private data class Dependencies(
        val runner: RunPalworldAutomation,
        val stop: StopPalworldServer,
    )

    private class MemoryStore : PalworldAutomationStore {
        private var runtime = PalworldAutomationRuntimeState()
        override fun loadSettings(): PalworldAutomationSettings? = SETTINGS
        override fun saveSettings(settings: PalworldAutomationSettings) = Unit
        override fun loadRuntime(): PalworldAutomationRuntimeState = runtime
        override fun saveRuntime(runtime: PalworldAutomationRuntimeState) {
            this.runtime = runtime
        }
    }

    private object EmptyHistoryStore : PalworldOperationHistoryStore {
        override fun append(entry: PalworldOperationHistoryEntry) = Unit
        override fun latest(limit: Int): List<PalworldOperationHistoryEntry> = emptyList()
    }

    private object FixedPasswordProvider : PalworldAdminPasswordProvider {
        override fun read(installPath: String): String = "admin-password"
    }

    companion object {
        private val SETTINGS = PalworldAutomationSettings(true, "04:00", "09:00", 8211, 3)
        private const val EXPECTED_INSTALL_PATH = "C:\\GameServerManager\\servers\\palworld\\main\\runtime"
    }
}
