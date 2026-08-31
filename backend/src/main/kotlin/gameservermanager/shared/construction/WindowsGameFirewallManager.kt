package gameservermanager.shared.construction

import com.fasterxml.jackson.databind.ObjectMapper
import gameservermanager.shared.construction.GameFirewallManager
import gameservermanager.shared.construction.GameFirewallRuleCommand
import gameservermanager.shared.configuration.StorageProperties
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.UUID

@Component
class WindowsGameFirewallManager(
    storageProperties: StorageProperties,
    private val objectMapper: ObjectMapper,
) : GameFirewallManager {
    private val exchangeDirectory = Path.of(storageProperties.root).toAbsolutePath().normalize()
        .resolve("config/firewall-helper")

    @Synchronized
    override fun apply(command: GameFirewallRuleCommand): String {
        validate(command)
        val ruleName = ruleName(command)
        execute("APPLY", ruleName, command.gamePort, command.remoteAddresses, command.protocol)
        return ruleName
    }

    @Synchronized
    override fun remove(command: GameFirewallRuleCommand) {
        validate(command)
        execute("REMOVE", ruleName(command), command.gamePort, command.remoteAddresses, command.protocol)
    }

    private fun execute(operation: String, ruleName: String, port: Int, addresses: List<String>, protocol: String) {
        require(System.getProperty("os.name").startsWith("Windows")) {
            "Windows Firewall操作はWindowsでのみ実行できます"
        }
        Files.createDirectories(exchangeDirectory)
        val requestId = UUID.randomUUID().toString()
        val requestPath = exchangeDirectory.resolve("request.json")
        val responsePath = exchangeDirectory.resolve("response.json")
        Files.deleteIfExists(responsePath)
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(
            requestPath.toFile(),
            Request(requestId, operation, ruleName, port, addresses, protocol),
        )

        val process = ProcessBuilder(
            "schtasks.exe", "/Run", "/TN", HELPER_TASK_NAME,
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText().trim()
        check(process.waitFor() == 0) {
            "Firewall補助タスクを起動できませんでした: $output"
        }

        val deadline = System.nanoTime() + RESPONSE_TIMEOUT.toNanos()
        while (System.nanoTime() < deadline) {
            if (Files.isRegularFile(responsePath)) {
                val response = objectMapper.readValue(responsePath.toFile(), Response::class.java)
                if (response.requestId == requestId) {
                    check(response.success) { response.message }
                    return
                }
            }
            Thread.sleep(POLL_INTERVAL.toMillis())
        }
        error("Firewall補助タスクの応答がタイムアウトしました。セットアップ状態を確認してください")
    }

    private fun validate(command: GameFirewallRuleCommand) {
        require(command.game.matches(IDENTIFIER_PATTERN)) { "ゲーム識別子が不正です" }
        require(command.serverId.matches(IDENTIFIER_PATTERN)) { "サーバー識別子が不正です" }
        require(command.gamePort in 1..65535) { "ゲームポートが不正です" }
        require(command.remoteAddresses.isNotEmpty()) { "Firewallの接続元を1つ以上指定してください" }
        require(command.protocol in setOf("UDP", "TCP")) { "Firewallプロトコルが不正です" }
    }

    private fun ruleName(command: GameFirewallRuleCommand): String {
        return "GameServerManager-Game-${command.game}-${command.serverId}-${command.protocol}-${command.gamePort}"
    }

    private data class Request(
        val requestId: String,
        val operation: String,
        val ruleName: String,
        val localPort: Int,
        val remoteAddresses: List<String>,
        val protocol: String,
    )

    private data class Response(
        val requestId: String = "",
        val success: Boolean = false,
        val message: String = "Firewall補助タスクの応答が不正です",
    )

    companion object {
        const val HELPER_TASK_NAME = "GameServerManager-FirewallHelper"
        private val IDENTIFIER_PATTERN = Regex("[A-Za-z0-9-]{1,64}")
        private val RESPONSE_TIMEOUT = Duration.ofSeconds(20)
        private val POLL_INTERVAL = Duration.ofMillis(200)
    }
}
