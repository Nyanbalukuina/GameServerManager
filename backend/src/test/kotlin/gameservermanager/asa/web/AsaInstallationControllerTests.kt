package gameservermanager.asa.web

import gameservermanager.asa.application.InstallAsaServer
import gameservermanager.shared.preflight.ManagedPathPolicy
import gameservermanager.shared.steamcmd.SteamCmdProcessResult
import gameservermanager.shared.steamcmd.SteamCmdProcessRunner
import gameservermanager.shared.error.ApiExceptionHandler
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator

// ASAインストールAPIのHTTPレスポンスを確認する。
class AsaInstallationControllerTests {
    @Test
    fun `ASAインストール結果を返す`() {
        // テスト専用の一時フォルダーを作成する。
        val root = Files.createTempDirectory("asa-controller-test")

        try {
            // SteamCMDとASAの保存先を準備する。
            val steamCmdPath = root.resolve("tools/steamcmd")
            val installPath = root.resolve("servers/asa/main/runtime")
            Files.createDirectories(steamCmdPath)
            Files.writeString(
                steamCmdPath.resolve("steamcmd.exe"),
                "test",
            )

            // 実際のSteamCMDを使わないテスト用APIを準備する。
            val installAsaServer = InstallAsaServer(
                TestManagedPathPolicy(root),
                TestProcessRunner(),
            )
            val mockMvc = MockMvcBuilders
                .standaloneSetup(
                    AsaInstallationController(installAsaServer),
                )
                .setControllerAdvice(ApiExceptionHandler())
                .build()

            // ASAインストールAPIへテスト用JSONを送信する。
            mockMvc.post("/api/asa/installations") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "steamCmdPath": ${jsonString(steamCmdPath.toString())},
                      "installPath": ${jsonString(installPath.toString())}
                    }
                """.trimIndent()
            }
                .andExpect {
                    // HTTP成功とASAのApp IDを確認する。
                    status { isOk() }
                    jsonPath("$.completed") { value(true) }
                    jsonPath("$.appId") { value(2430930) }
                    jsonPath("$.exitCode") { value(0) }
                }
        } finally {
            // テスト終了後に一時フォルダーを削除する。
            Files.walk(root).use { paths ->
                paths
                    .sorted(Comparator.reverseOrder())
                    .forEach(Files::deleteIfExists)
            }
        }
    }

    // WindowsパスをJSONへ安全に埋め込める文字列に変換する。
    private fun jsonString(value: String): String {
        return "\"${value.replace("\\", "\\\\")}\""
    }

    // 実際のSteamCMDを起動せず、インストール成功を再現する。
    private class TestProcessRunner : SteamCmdProcessRunner {
        override fun run(
            executable: Path,
            arguments: List<String>,
            logPath: Path,
        ): SteamCmdProcessResult {
            val installPath = Path.of(arguments[1])
            val gameExecutable = installPath
                .resolve("ShooterGame")
                .resolve("Binaries")
                .resolve("Win64")
                .resolve("ArkAscendedServer.exe")

            Files.createDirectories(requireNotNull(gameExecutable.parent))
            Files.writeString(gameExecutable, "test")
            Files.writeString(logPath, "test")

            return SteamCmdProcessResult(
                exitCode = 0,
                logPath = logPath.toString(),
            )
        }
    }

    // 一時フォルダー内だけを管理対象として許可する。
    private class TestManagedPathPolicy(
        root: Path,
    ) : ManagedPathPolicy {
        private val normalizedRoot = root.toAbsolutePath().normalize()

        override fun isServerPathAllowed(path: String): Boolean {
            return Path.of(path)
                .toAbsolutePath()
                .normalize()
                .startsWith(normalizedRoot.resolve("servers"))
        }

        override fun isToolPathAllowed(path: String): Boolean {
            return Path.of(path)
                .toAbsolutePath()
                .normalize()
                .startsWith(normalizedRoot.resolve("tools"))
        }

        override fun managedRoot(): String {
            return normalizedRoot.toString()
        }
    }
}