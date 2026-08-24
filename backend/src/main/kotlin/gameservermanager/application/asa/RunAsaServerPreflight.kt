package gameservermanager.application.asa

import gameservermanager.application.preflight.RunServerPreflight
import gameservermanager.application.preflight.ServerEnvironmentInspector
import gameservermanager.domain.preflight.PreflightCheck
import gameservermanager.domain.preflight.PreflightStatus
import gameservermanager.domain.preflight.ServerPreflightReport
import org.springframework.stereotype.Service

// 共通の構築前検証へASA固有のPeer・Queryポート検査を追加する。
@Service
class RunAsaServerPreflight(
    private val commonPreflight: RunServerPreflight,
    private val environmentInspector: ServerEnvironmentInspector,
) {
    fun execute(command: Command): ServerPreflightReport {
        val common = commonPreflight.execute(
            RunServerPreflight.Command(command.installPath, command.steamCmdPath, command.gamePort, command.rconPort),
        )
        val peerPort = command.gamePort + 1
        val checks = common.checks + listOf(
            udpCheck("port.peer", "Peerポート", peerPort),
            udpCheck("port.query", "Queryポート", command.queryPort),
            distinctPorts(command, peerPort),
        )
        return ServerPreflightReport(checks.none { it.status == PreflightStatus.ERROR }, checks)
    }

    private fun udpCheck(id: String, label: String, port: Int): PreflightCheck {
        return if (environmentInspector.isUdpPortAvailable(port)) {
            PreflightCheck(id, "$label UDP $port", PreflightStatus.PASS, "使用できます")
        } else {
            PreflightCheck(id, "$label UDP $port", PreflightStatus.ERROR, "既に使用されています")
        }
    }

    private fun distinctPorts(command: Command, peerPort: Int): PreflightCheck {
        val ports = listOf(command.gamePort, peerPort, command.queryPort, command.rconPort)
        return if (ports.distinct().size == ports.size) {
            PreflightCheck("ports.distinct", "ポートの重複", PreflightStatus.PASS, "すべて異なるポートです")
        } else {
            PreflightCheck("ports.distinct", "ポートの重複", PreflightStatus.ERROR, "各ポートを異なる番号にしてください")
        }
    }

    data class Command(
        val installPath: String,
        val steamCmdPath: String,
        val gamePort: Int,
        val queryPort: Int,
        val rconPort: Int,
    )
}
