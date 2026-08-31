package gameservermanager.shared.server

import java.nio.file.Path

// ゲームサーバープロセスの起動・状態確認・停止を定義する。
interface GameServerProcessManager {
    fun start(command: GameServerProcessCommand): GameServerProcessSnapshot
    fun attach(serverId: String, processId: Long, logPath: Path): GameServerProcessSnapshot {
        throw UnsupportedOperationException("既存プロセスの再接続には対応していません")
    }
    fun current(serverId: String): GameServerProcessSnapshot?
    fun stop(serverId: String)
}

// ゲームサーバープロセスの起動に必要な情報を保持する。
data class GameServerProcessCommand(
    val serverId: String,
    val executable: Path,
    val workingDirectory: Path,
    val arguments: List<String>,
    val logPath: Path,
)

// 起動したゲームサーバープロセスの状態を保持する。
data class GameServerProcessSnapshot(
    val serverId: String,
    val processId: Long,
    val alive: Boolean,
    val exitCode: Int?,
    val logPath: String,
)
