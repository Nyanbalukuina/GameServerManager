package gameservermanager.application.preflight

import gameservermanager.domain.preflight.PreflightStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RunServerPreflightTests {
    @Test
    fun `構築可能な環境ではエラーなしの結果を返す`() {
        val inspector = FakeEnvironmentInspector()
        val useCase = RunServerPreflight(inspector)

        val report = useCase.execute(
            RunServerPreflight.Command(
                installPath = "C:\\GameServers\\Palworld",
                steamCmdPath = "C:\\GameServers\\SteamCMD",
                gamePort = 8211,
                rconPort = 25575,
            ),
        )

        assertThat(report.canProceed).isTrue()
        assertThat(report.checks).noneMatch { it.status == PreflightStatus.ERROR }
        assertThat(report.checks).anyMatch { it.id == "steamcmd.installed" }
    }

    @Test
    fun `保存先重複と使用中ポートをエラーにする`() {
        val inspector = FakeEnvironmentInspector(
            udpPortAvailable = false,
            tcpPortAvailable = false,
        )
        val useCase = RunServerPreflight(inspector)

        val report = useCase.execute(
            RunServerPreflight.Command(
                installPath = "C:\\GameServers",
                steamCmdPath = "c:\\gameservers\\",
                gamePort = 8211,
                rconPort = 25575,
            ),
        )

        assertThat(report.canProceed).isFalse()
        assertThat(report.checks.filter { it.status == PreflightStatus.ERROR }.map { it.id })
            .contains("paths.distinct", "port.game", "port.rcon")
    }

    @Test
    fun `空き容量が10GiB未満の場合はエラーにする`() {
        val inspector = FakeEnvironmentInspector(usableSpaceBytes = 9L * GIBIBYTE)
        val useCase = RunServerPreflight(inspector)

        val report = useCase.execute(
            RunServerPreflight.Command(
                installPath = "C:\\GameServers\\Palworld",
                steamCmdPath = "C:\\GameServers\\SteamCMD",
                gamePort = 8211,
                rconPort = 25575,
            ),
        )

        assertThat(report.checks.single { it.id == "storage.space" }.status)
            .isEqualTo(PreflightStatus.ERROR)
    }

    private class FakeEnvironmentInspector(
        private val usableSpaceBytes: Long = 100L * GIBIBYTE,
        private val udpPortAvailable: Boolean = true,
        private val tcpPortAvailable: Boolean = true,
    ) : ServerEnvironmentInspector {
        override fun inspectPath(path: String, executableName: String?) =
            PathInspection(
                valid = true,
                absolute = true,
                root = false,
                exists = false,
                directory = true,
                writable = true,
                usableSpaceBytes = usableSpaceBytes,
                executableExists = false,
            )

        override fun isUdpPortAvailable(port: Int) = udpPortAvailable

        override fun isTcpPortAvailable(port: Int) = tcpPortAvailable
    }

    companion object {
        private const val GIBIBYTE = 1024L * 1024L * 1024L
    }
}

