package gameservermanager.web.palworld

import gameservermanager.application.palworld.InstallPalworldServer
import gameservermanager.application.palworld.SteamCmdProcessResult
import gameservermanager.application.palworld.SteamCmdProcessRunner
import gameservermanager.application.preflight.ManagedPathPolicy
import gameservermanager.web.error.ApiExceptionHandler
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.nio.file.Files
import java.nio.file.Path

class PalworldInstallationControllerTests {
    @Test
    fun `Palworldインストール結果を返す`() {
        val root = Files.createTempDirectory("palworld-controller-test")
        try {
            val steamCmdPath = root.resolve("tools/steamcmd")
            val installPath = root.resolve("servers/palworld/main/runtime")
            Files.createDirectories(steamCmdPath)
            Files.writeString(steamCmdPath.resolve("steamcmd.exe"), "test")
            val useCase = InstallPalworldServer(
                TestManagedPathPolicy(root),
                TestProcessRunner(),
            )
            val mockMvc = MockMvcBuilders
                .standaloneSetup(PalworldInstallationController(useCase))
                .setControllerAdvice(ApiExceptionHandler())
                .build()

            mockMvc.post("/api/palworld/installations") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "steamCmdPath": ${jsonString(steamCmdPath.toString())},
                      "installPath": ${jsonString(installPath.toString())}
                    }
                """.trimIndent()
            }
                .andExpect {
                    status { isOk() }
                    jsonPath("$.completed") { value(true) }
                    jsonPath("$.appId") { value(2394010) }
                    jsonPath("$.exitCode") { value(0) }
                }
        } finally {
            Files.walk(root).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }

    private fun jsonString(value: String): String {
        return "\"${value.replace("\\", "\\\\")}\""
    }

    private class TestProcessRunner : SteamCmdProcessRunner {
        override fun run(
            executable: Path,
            arguments: List<String>,
            logPath: Path,
        ): SteamCmdProcessResult {
            Files.writeString(logPath, "test")
            Files.writeString(Path.of(arguments[1]).resolve("PalServer.exe"), "test")
            return SteamCmdProcessResult(0, logPath.toString())
        }
    }

    private class TestManagedPathPolicy(private val root: Path) : ManagedPathPolicy {
        override fun isServerPathAllowed(path: String): Boolean {
            return Path.of(path).startsWith(root.resolve("servers"))
        }

        override fun isToolPathAllowed(path: String): Boolean {
            return Path.of(path).startsWith(root.resolve("tools"))
        }

        override fun managedRoot(): String {
            return root.toString()
        }
    }
}
