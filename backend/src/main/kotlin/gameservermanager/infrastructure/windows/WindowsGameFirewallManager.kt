package gameservermanager.infrastructure.windows

import com.fasterxml.jackson.databind.ObjectMapper
import gameservermanager.application.construction.GameFirewallManager
import gameservermanager.application.construction.GameFirewallRuleCommand
import gameservermanager.configuration.StorageProperties
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
        execute("APPLY", ruleName, command.gamePort, command.remoteAddresses)
        return ruleName
    }

    @Synchronized
    override fun remove(command: GameFirewallRuleCommand) {
        validate(command)
        execute("REMOVE", ruleName(command), command.gamePort, command.remoteAddresses)
    }

    private fun execute(operation: String, ruleName: String, port: Int, addresses: List<String>) {
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
            Request(requestId, operation, ruleName, port, addresses),
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
    }

    private fun ruleName(command: GameFirewallRuleCommand): String {
        return "GameServerManager-Game-${command.game}-${command.serverId}-UDP-${command.gamePort}"
    }

    private data class Request(
        val requestId: String,
        val operation: String,
        val ruleName: String,
        val localPort: Int,
        val remoteAddresses: List<String>,
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
