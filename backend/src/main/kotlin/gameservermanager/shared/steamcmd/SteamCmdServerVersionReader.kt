package gameservermanager.shared.steamcmd

import gameservermanager.shared.preflight.ManagedPathPolicy
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class SteamCmdServerVersionReader(
    private val managedPathPolicy: ManagedPathPolicy,
    private val processRunner: SteamCmdProcessRunner,
) {
    fun currentBuildId(gameName: String, appId: Int, steamCmdPath: String, installPath: String): String {
        val paths = normalizedPaths(steamCmdPath, installPath)
        val manifestName = "appmanifest_$appId.acf"
        val manifest = listOf(
            paths.steamCmd.resolve("steamapps").resolve(manifestName),
            paths.install.resolve("steamapps").resolve(manifestName),
        ).firstOrNull(Files::isRegularFile)
            ?: throw IllegalStateException("${gameName}サーバーのBuild IDを記録したSteam manifestが見つかりません")
        return parseInstalledBuildId(Files.readString(manifest))
    }

    fun latestBuildId(gameName: String, appId: Int, steamCmdPath: String, installPath: String): String {
        val paths = normalizedPaths(steamCmdPath, installPath)
        val executable = paths.steamCmd.resolve("steamcmd.exe")
        require(Files.isRegularFile(executable)) { "steamcmd.exeが見つかりません" }
        val logPath = paths.steamCmd.resolve("logs/${gameName.lowercase()}-version-check.log")
        Files.deleteIfExists(logPath)
        val arguments = listOf(
            "+force_install_dir", paths.install.toString(),
            "+login", "anonymous",
            "+app_info_update", "1",
            "+app_info_print", appId.toString(),
            "+quit",
        )
        var result = processRunner.run(executable, arguments, logPath)
        if (result.exitCode == STEAMCMD_FIRST_LAUNCH_EXIT_CODE) {
            Files.deleteIfExists(logPath)
            result = processRunner.run(executable, arguments, logPath)
        }
        check(result.exitCode == 0) {
            "最新Build IDの取得に失敗しました。SteamCMD終了コード: ${result.exitCode}、ログ: ${result.logPath}"
        }
        return parseLatestBuildId(Files.readString(logPath))
    }

    internal fun parseInstalledBuildId(content: String): String {
        return BUILD_ID.find(content)?.groupValues?.get(1)
            ?: throw IllegalStateException("Steam manifestから現在のBuild IDを取得できませんでした")
    }

    internal fun parseLatestBuildId(content: String): String {
        return PUBLIC_BUILD_ID.find(content)?.groupValues?.get(1)
            ?: throw IllegalStateException("SteamCMDから最新のBuild IDを取得できませんでした")
    }

    private fun normalizedPaths(steamCmdPath: String, installPath: String): Paths {
        require(managedPathPolicy.isToolPathAllowed(steamCmdPath)) { "SteamCMD保存先が管理範囲外です" }
        require(managedPathPolicy.isServerPathAllowed(installPath)) { "サーバーインストール先が管理範囲外です" }
        return Paths(
            Path.of(steamCmdPath).toAbsolutePath().normalize(),
            Path.of(installPath).toAbsolutePath().normalize(),
        )
    }

    private data class Paths(val steamCmd: Path, val install: Path)

    companion object {
        private const val STEAMCMD_FIRST_LAUNCH_EXIT_CODE = 7
        private val BUILD_ID = Regex("\"buildid\"\\s+\"(\\d+)\"")
        private val PUBLIC_BUILD_ID = Regex("\"public\"\\s*\\{[^}]*\"buildid\"\\s+\"(\\d+)\"", RegexOption.DOT_MATCHES_ALL)
    }
}
