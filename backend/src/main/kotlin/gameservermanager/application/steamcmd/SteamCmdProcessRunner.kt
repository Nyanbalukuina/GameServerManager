package gameservermanager.application.steamcmd

import java.nio.file.Path

// SteamCMDを実行し、終了結果を返す共通インターフェース。
interface SteamCmdProcessRunner {
    fun run(
        executable: Path,
        arguments: List<String>,
        logPath: Path,
    ): SteamCmdProcessResult
}

// SteamCMDの終了コードとログの保存先を保持する。
data class SteamCmdProcessResult(
    val exitCode: Int,
    val logPath: String,
)