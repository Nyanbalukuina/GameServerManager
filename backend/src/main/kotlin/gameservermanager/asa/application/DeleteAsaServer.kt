package gameservermanager.asa.application

import gameservermanager.shared.construction.GameFirewallManager
import gameservermanager.shared.construction.GameFirewallRuleCommand
import gameservermanager.shared.preflight.ManagedPathPolicy
import gameservermanager.shared.server.GameServerRegistrationStore
import gameservermanager.shared.server.GameServerRegistration
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator

// 停止中のASAサーバーについてFirewall・実体・管理登録を安全に削除する。
@Service
class DeleteAsaServer(
    private val registrationStore: GameServerRegistrationStore,
    private val managedPathPolicy: ManagedPathPolicy,
    private val firewallManager: GameFirewallManager,
) {
    @Synchronized
    fun execute(confirmation: String) {
        require(confirmation == "ASA") { "削除確認にはASAと入力してください" }
        val server = requireNotNull(registrationStore.findByGame("ASA")) { "ASAサーバーは登録されていません" }
        require(server.state == "STOPPED") { "ASAサーバーを停止してから削除してください" }

        if (server.mode == "REAL") {
            firewallCommands(server).forEach(firewallManager::remove)
        }
        deleteTree(deleteTarget(server))
        registrationStore.delete("ASA")
    }

    private fun deleteTarget(server: GameServerRegistration): Path {
        if (server.mode == "DEMO") {
            val workspace = Path.of(server.workspacePath).toAbsolutePath().normalize()
            val expected = Path.of(System.getProperty("java.io.tmpdir"))
                .resolve("GameServerManagerDemo").toAbsolutePath().normalize()
            require(workspace == expected) { "ASAデモ領域以外は削除できません" }
            return workspace.resolve("servers/asa/main")
        }

        require(managedPathPolicy.isServerPathAllowed(server.installPath)) { "ASAインストール先が管理範囲外です" }
        return Path.of(server.installPath).toAbsolutePath().normalize()
    }

    private fun firewallCommands(server: GameServerRegistration): List<GameFirewallRuleCommand> {
        val ports = listOf(server.gamePort, requireNotNull(server.peerPort), requireNotNull(server.queryPort))
        return ports.map { GameFirewallRuleCommand("ASA", server.serverId, it, server.gamePortAccess.remoteAddresses()) }
    }

    private fun deleteTree(target: Path) {
        if (!Files.exists(target)) return
        Files.walk(target).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }
}
