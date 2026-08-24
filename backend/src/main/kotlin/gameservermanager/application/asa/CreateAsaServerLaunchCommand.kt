package gameservermanager.application.asa

import gameservermanager.application.preflight.ManagedPathPolicy
import org.springframework.stereotype.Service
import java.nio.file.Path

// ASAサーバープロセスの起動に必要な情報を保持する。
data class AsaServerLaunchCommand(
    val executable: Path,
    val arguments: List<String>,
    val logPath: Path,
    val gamePort: Int,
    val peerPort: Int,
    val queryPort: Int,
)

// ASA専用サーバーの起動引数を組み立てる。
@Service
class CreateAsaServerLaunchCommand(private val managedPathPolicy: ManagedPathPolicy) {
    // 入力値を検証してASAの起動情報を作成する。
    fun execute(command: Command): AsaServerLaunchCommand {
        require(managedPathPolicy.isServerPathAllowed(command.installPath)) {
            "ASAインストール先が管理範囲外です"
        }
        require(command.map == THE_ISLAND_MAP) {
            "現在対応しているマップはThe Islandだけです"
        }
        require(command.gamePort in 1..65534) {
            "ゲームポートは1から65534の範囲で指定してください"
        }
        require(command.queryPort in 1..65535) {
            "Queryポートは1から65535の範囲で指定してください"
        }
        require(command.maxPlayers in 1..70) {
            "最大プレイヤー数は1から70の範囲で指定してください"
        }

        // Peerポートはゲームポートの次の番号を使用する。
        val peerPort = command.gamePort + 1
        require(command.queryPort != command.gamePort && command.queryPort != peerPort) {
            "QueryポートはゲームポートおよびPeerポートと分けてください"
        }

        val installPath = Path.of(command.installPath).toAbsolutePath().normalize()
        val executable = installPath
            .resolve("ShooterGame")
            .resolve("Binaries")
            .resolve("Win64")
            .resolve("ArkAscendedServer.exe")
        val logPath = Path.of(managedPathPolicy.managedRoot())
            .resolve("logs/asa-main.log")

        // ASA仕様に合わせてマップ、Queryポート、ゲームポート、最大人数を指定する。
        val arguments = listOf(
            "${command.map}?QueryPort=${command.queryPort}",
            "-port=${command.gamePort}",
            "-WinLiveMaxPlayers=${command.maxPlayers}",
        )

        return AsaServerLaunchCommand(
            executable = executable,
            arguments = arguments,
            logPath = logPath,
            gamePort = command.gamePort,
            peerPort = peerPort,
            queryPort = command.queryPort,
        )
    }

    // 起動コマンドの作成に必要な入力値を保持する。
    data class Command(
        val installPath: String,
        val map: String,
        val gamePort: Int,
        val queryPort: Int,
        val maxPlayers: Int,
    )

    companion object {
        const val THE_ISLAND_MAP = "TheIsland_WP"
    }
}