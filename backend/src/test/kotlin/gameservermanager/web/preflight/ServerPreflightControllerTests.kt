package gameservermanager.web.preflight

import gameservermanager.application.preflight.RunServerPreflight
import gameservermanager.domain.preflight.PreflightCheck
import gameservermanager.domain.preflight.PreflightStatus
import gameservermanager.domain.preflight.ServerPreflightReport
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(ServerPreflightController::class)
@AutoConfigureMockMvc(addFilters = false)
class ServerPreflightControllerTests @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var runServerPreflight: RunServerPreflight

    @Test
    fun `事前検証結果を返す`() {
        val command = RunServerPreflight.Command(
            installPath = "C:\\GameServers\\Palworld",
            steamCmdPath = "C:\\GameServers\\SteamCMD",
            gamePort = 8211,
            rconPort = 25575,
        )
        given(runServerPreflight.execute(command)).willReturn(
            ServerPreflightReport(
                canProceed = true,
                checks = listOf(
                    PreflightCheck("port.game", "ゲームポート", PreflightStatus.PASS, "使用できます"),
                ),
            ),
        )

        mockMvc.post("/api/server-construction-preflight") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "installPath": "C:\\GameServers\\Palworld",
                  "steamCmdPath": "C:\\GameServers\\SteamCMD",
                  "gamePort": 8211,
                  "rconPort": 25575
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.canProceed") { value(true) }
            jsonPath("$.checks[0].status") { value("PASS") }
        }
    }
}
